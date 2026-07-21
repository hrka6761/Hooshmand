package ir.hrka.hooshmand.ai_chat.impl.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import ir.hrka.persian.tts.api.PersianTts
import ir.hrka.persian.tts.api.PersianTtsFactory
import ir.hrka.persian.tts.api.PersianTtsResult
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Result of requesting speech for a chat message.
 */
internal sealed interface AiChatSpeakResult {
    /** Playback started, preparation started, or an in-progress utterance was stopped. */
    data object Ok : AiChatSpeakResult

    /** Message body was empty after stripping markdown. */
    data object EmptyText : AiChatSpeakResult

    /** Device has no offline voice pack for the detected language. */
    data class LanguageUnavailable(
        val languageTag: String,
    ) : AiChatSpeakResult

    /** Speech playback failed. */
    data object SpeakFailed : AiChatSpeakResult
}

/**
 * Offline text-to-speech for AI chat replies.
 *
 * - Persian (`fa`) uses only the bundled [PersianTts] engine.
 * - Arabic (`ar`) and English (`en`) use only the device [TextToSpeech] engine.
 */
internal class AiChatTextToSpeech(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val systemReady = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var persianBridgeJob: Job? = null
    private var pendingPersianJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    private val _speakingMessageId = MutableStateFlow<String?>(null)
    private val _preparingMessageId = MutableStateFlow<String?>(null)

    /** `true` while an utterance is in progress. */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /** Id of the message currently being spoken, or `null`. */
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    /**
     * Id of the message waiting for the Persian engine to become ready, or `null`.
     *
     * While set, the UI should show a progress indicator instead of an error.
     */
    val preparingMessageId: StateFlow<String?> = _preparingMessageId.asStateFlow()

    private val persianTts: PersianTts = PersianTtsFactory.create(appContext)
    private var systemTts: TextToSpeech? = null

    init {
        persianBridgeJob =
            scope.launch {
                combine(
                    persianTts.isSpeaking,
                    persianTts.speakingUtteranceId,
                    _preparingMessageId,
                ) { speaking, utteranceId, preparingId ->
                    Triple(speaking, utteranceId, preparingId)
                }.collect { (speaking, utteranceId, preparingId) ->
                    if (speaking && utteranceId != null) {
                        _isSpeaking.value = true
                        _speakingMessageId.value = utteranceId
                    } else if (
                        !speaking &&
                        preparingId == null &&
                        _speakingMessageId.value != null &&
                        systemTts?.isSpeaking != true
                    ) {
                        _isSpeaking.value = false
                        _speakingMessageId.value = null
                    }
                }
            }

        systemTts =
            TextToSpeech(appContext) { status ->
                systemReady.set(status == TextToSpeech.SUCCESS)
                val engine = systemTts
                if (status == TextToSpeech.SUCCESS && engine != null) {
                    engine.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                                if (!utteranceId.isNullOrBlank()) {
                                    _speakingMessageId.value = utteranceId
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                                _speakingMessageId.value = null
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                                _speakingMessageId.value = null
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isSpeaking.value = false
                                _speakingMessageId.value = null
                            }
                        },
                    )
                }
            }
    }

    /**
     * Speaks [text] for [messageId], choosing Persian, Arabic, or English from the script.
     *
     * Stops any in-progress utterance first. If the same [messageId] is already speaking or
     * preparing, stops instead (toggle).
     */
    fun speakOrStop(
        messageId: String,
        text: String,
    ): AiChatSpeakResult {
        val isActiveForMessage =
            (_speakingMessageId.value == messageId && _isSpeaking.value) ||
                _preparingMessageId.value == messageId
        if (isActiveForMessage) {
            stop()
            return AiChatSpeakResult.Ok
        }

        val plain = plainTextForSpeech(text)
        if (plain.isBlank()) {
            return AiChatSpeakResult.EmptyText
        }

        val locale = detectSpeechLocale(plain)

        return when (locale.language) {
            "fa" -> speakPersian(messageId, plain)
            else -> speakWithSystemTts(messageId, plain, locale)
        }
    }

    private fun speakPersian(
        messageId: String,
        plain: String,
    ): AiChatSpeakResult {
        stopSystemTtsOnly()
        cancelPendingPersian()

        if (persianTts.isReady.value) {
            return dispatchPersianSpeak(messageId, plain)
        }

        // Wait for the engine without surfacing an error to the user.
        _preparingMessageId.value = messageId
        pendingPersianJob =
            scope.launch {
                try {
                    val ready =
                        withTimeoutOrNull(PERSIAN_READY_TIMEOUT_MS) {
                            persianTts.isReady.first { it }
                        }
                    if (_preparingMessageId.value != messageId) {
                        return@launch
                    }
                    if (ready != true) {
                        _preparingMessageId.value = null
                        return@launch
                    }
                    _preparingMessageId.value = null
                    dispatchPersianSpeak(messageId, plain)
                } catch (_: Throwable) {
                    if (_preparingMessageId.value == messageId) {
                        _preparingMessageId.value = null
                    }
                }
            }
        return AiChatSpeakResult.Ok
    }

    private fun dispatchPersianSpeak(
        messageId: String,
        plain: String,
    ): AiChatSpeakResult =
        when (
            persianTts.speakOrStop(
                utteranceId = messageId,
                text = plain,
            )
        ) {
            PersianTtsResult.Ok -> AiChatSpeakResult.Ok
            PersianTtsResult.EngineNotReady -> {
                // Should be rare after waiting; queue again rather than erroring.
                _preparingMessageId.value = messageId
                pendingPersianJob =
                    scope.launch {
                        val ready =
                            withTimeoutOrNull(PERSIAN_READY_TIMEOUT_MS) {
                                persianTts.isReady.first { it }
                            }
                        if (_preparingMessageId.value != messageId) return@launch
                        _preparingMessageId.value = null
                        if (ready == true) {
                            persianTts.speakOrStop(messageId, plain)
                        }
                    }
                AiChatSpeakResult.Ok
            }

            PersianTtsResult.EmptyText -> AiChatSpeakResult.EmptyText
            is PersianTtsResult.SpeakFailed -> AiChatSpeakResult.SpeakFailed
        }

    private fun speakWithSystemTts(
        messageId: String,
        plain: String,
        locale: Locale,
    ): AiChatSpeakResult {
        cancelPendingPersian()
        _preparingMessageId.value = null

        if (!systemReady.get()) {
            // Queue briefly until system TTS finishes init (same UX as Persian preparing).
            _preparingMessageId.value = messageId
            pendingPersianJob =
                scope.launch {
                    val ready =
                        withTimeoutOrNull(SYSTEM_READY_TIMEOUT_MS) {
                            while (!systemReady.get()) {
                                kotlinx.coroutines.delay(50L)
                            }
                            true
                        }
                    if (_preparingMessageId.value != messageId) return@launch
                    _preparingMessageId.value = null
                    if (ready == true) {
                        speakWithSystemTtsNow(messageId, plain, locale)
                    }
                }
            return AiChatSpeakResult.Ok
        }

        return speakWithSystemTtsNow(messageId, plain, locale)
    }

    private fun speakWithSystemTtsNow(
        messageId: String,
        plain: String,
        locale: Locale,
    ): AiChatSpeakResult {
        val engine = systemTts ?: return AiChatSpeakResult.SpeakFailed

        val languageStatus = engine.setLanguage(locale)
        if (
            languageStatus == TextToSpeech.LANG_MISSING_DATA ||
            languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return AiChatSpeakResult.LanguageUnavailable(
                languageTag = locale.toLanguageTag(),
            )
        }

        persianTts.stop()
        engine.setSpeechRate(1.0f)
        engine.setPitch(1.0f)

        stopSystemTtsOnly()
        _speakingMessageId.value = messageId
        val queued =
            engine.speak(
                plain,
                TextToSpeech.QUEUE_FLUSH,
                /* params = */ null,
                /* utteranceId = */ messageId,
            )
        if (queued == TextToSpeech.ERROR) {
            _speakingMessageId.value = null
            _isSpeaking.value = false
            return AiChatSpeakResult.SpeakFailed
        }
        return AiChatSpeakResult.Ok
    }

    /** Stops the current utterance / preparation if any. */
    fun stop() {
        cancelPendingPersian()
        _preparingMessageId.value = null
        persianTts.stop()
        stopSystemTtsOnly()
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    private fun cancelPendingPersian() {
        pendingPersianJob?.cancel()
        pendingPersianJob = null
    }

    private fun stopSystemTtsOnly() {
        val engine = systemTts
        if (engine != null) {
            runCatching {
                if (engine.isSpeaking) {
                    engine.stop()
                }
            }
        }
    }

    /** Releases the underlying TTS engines. */
    fun shutdown() {
        stop()
        systemReady.set(false)
        persianBridgeJob?.cancel()
        persianBridgeJob = null
        scope.cancel()
        runCatching { persianTts.shutdown() }
        runCatching { systemTts?.shutdown() }
        systemTts = null
    }

    companion object {
        private const val PERSIAN_READY_TIMEOUT_MS = 60_000L
        private const val SYSTEM_READY_TIMEOUT_MS = 10_000L

        private val LOCALE_FA = Locale.forLanguageTag("fa")
        private val LOCALE_AR = Locale.forLanguageTag("ar")

        /**
         * Detects the TTS language for [text].
         *
         * - Persian-only letters (پ چ ژ گ) → Persian
         * - Other Arabic-script text → Arabic (system TTS only)
         * - Otherwise → English
         */
        fun detectSpeechLocale(text: String): Locale {
            var persianOnlyLetters = 0
            var arabicScript = 0
            var latinScript = 0

            var index = 0
            while (index < text.length) {
                val codePoint = text.codePointAt(index)
                index += Character.charCount(codePoint)
                when {
                    isPersianOnlyLetter(codePoint) -> {
                        persianOnlyLetters++
                        arabicScript++
                    }

                    Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.ARABIC ->
                        arabicScript++

                    Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN ->
                        latinScript++
                }
            }

            return when {
                persianOnlyLetters > 0 -> LOCALE_FA
                arabicScript > latinScript ->
                    // Prefer Persian TTS when the app/system language is Persian; otherwise Arabic.
                    if (Locale.getDefault().language == "fa") {
                        LOCALE_FA
                    } else {
                        LOCALE_AR
                    }

                else -> Locale.ENGLISH
            }
        }

        /** Ordered locale candidates kept for callers that need a list. */
        fun speechLocaleCandidates(text: String): List<Locale> =
            listOf(detectSpeechLocale(text))

        /** Strips common markdown so TTS reads natural prose. */
        fun plainTextForSpeech(markdown: String): String =
            markdown
                .replace(Regex("```[\\s\\S]*?```"), " ")
                .replace(Regex("`([^`]+)`"), "$1")
                .replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), " ")
                .replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
                .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
                .replace(Regex("(\\*\\*|__|\\*|_|~~)"), "")
                .replace(Regex("^\\s*[-*+]\\s+", RegexOption.MULTILINE), "")
                .replace(Regex("^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), "")
                // Keep song/lyric structure: lines → soft pause, paragraphs → stop.
                .replace(Regex("\\r\\n?"), "\n")
                .replace(Regex("\\n{2,}"), ". ")
                .replace(Regex("\\n"), "، ")
                .replace(Regex("[\\t\\f\\v]+"), " ")
                .replace(Regex(" +"), " ")
                .trim()

        private fun isPersianOnlyLetter(codePoint: Int): Boolean =
            codePoint == 0x067E || // پ
                codePoint == 0x0686 || // چ
                codePoint == 0x0698 || // ژ
                codePoint == 0x06AF // گ
    }
}
