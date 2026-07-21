package ir.hrka.hooshmand.ai_chat.impl.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Result of requesting speech for a chat message.
 */
internal sealed interface AiChatSpeakResult {
    /** Playback started, or an in-progress utterance for the same message was stopped. */
    data object Ok : AiChatSpeakResult

    /** TTS engine has not finished initializing yet. */
    data object EngineNotReady : AiChatSpeakResult

    /** Message body was empty after stripping markdown. */
    data object EmptyText : AiChatSpeakResult

    /** Device has no offline voice pack for the detected language. */
    data class LanguageUnavailable(
        val languageTag: String,
    ) : AiChatSpeakResult

    /** [TextToSpeech.speak] failed. */
    data object SpeakFailed : AiChatSpeakResult
}

/**
 * Offline text-to-speech for AI chat replies using the device [TextToSpeech] engine.
 *
 * Supports Persian (`fa`), Arabic (`ar`), and English (`en`) when the matching offline
 * voice data is installed on the device. No network calls are made by this class.
 */
internal class AiChatTextToSpeech(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val ready = AtomicBoolean(false)
    private val _isSpeaking = MutableStateFlow(false)
    private val _speakingMessageId = MutableStateFlow<String?>(null)

    /** `true` while an utterance is in progress. */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /** Id of the message currently being spoken, or `null`. */
    val speakingMessageId: StateFlow<String?> = _speakingMessageId.asStateFlow()

    private lateinit var tts: TextToSpeech

    init {
        tts =
            TextToSpeech(appContext) { status ->
                ready.set(status == TextToSpeech.SUCCESS)
                if (status == TextToSpeech.SUCCESS && ::tts.isInitialized) {
                    tts.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
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
     * Stops any in-progress utterance first. If the same [messageId] is already speaking,
     * stops playback instead (toggle).
     */
    fun speakOrStop(
        messageId: String,
        text: String,
    ): AiChatSpeakResult {
        if (_speakingMessageId.value == messageId && _isSpeaking.value) {
            stop()
            return AiChatSpeakResult.Ok
        }

        if (!ready.get()) {
            return AiChatSpeakResult.EngineNotReady
        }

        val plain = plainTextForSpeech(text)
        if (plain.isBlank()) {
            return AiChatSpeakResult.EmptyText
        }

        val localeCandidates = speechLocaleCandidates(plain)
        val selectedLocale =
            localeCandidates.firstOrNull { locale ->
                val result = tts.setLanguage(locale)
                result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        if (selectedLocale == null) {
            return AiChatSpeakResult.LanguageUnavailable(
                languageTag = localeCandidates.first().toLanguageTag(),
            )
        }

        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)

        stop()
        _speakingMessageId.value = messageId
        val queued =
            tts.speak(
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

    /** Stops the current utterance if any. */
    fun stop() {
        if (::tts.isInitialized && tts.isSpeaking) {
            tts.stop()
        }
        _isSpeaking.value = false
        _speakingMessageId.value = null
    }

    /** Releases the underlying TTS engine. */
    fun shutdown() {
        stop()
        ready.set(false)
        if (::tts.isInitialized) {
            tts.shutdown()
        }
    }

    companion object {
        private val LOCALE_FA = Locale.forLanguageTag("fa")
        private val LOCALE_AR = Locale.forLanguageTag("ar")

        /**
         * Ordered TTS locale candidates for [text].
         *
         * Only letters that do **not** appear in standard Arabic (پ چ ژ گ) force Persian first.
         * Shared Arabic-script letters such as ی/ک are common in both languages and must not
         * classify text as Persian — otherwise Arabic replies pick the wrong voice.
         */
        fun speechLocaleCandidates(text: String): List<Locale> {
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
                persianOnlyLetters > 0 -> listOf(LOCALE_FA, LOCALE_AR)
                arabicScript > latinScript -> listOf(LOCALE_AR, LOCALE_FA)
                else -> listOf(Locale.ENGLISH)
            }
        }

        /** Prefer the primary locale from [speechLocaleCandidates]. */
        fun detectSpeechLocale(text: String): Locale =
            speechLocaleCandidates(text).first()

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
                .replace(Regex("\\s+"), " ")
                .trim()

        /**
         * Letters used in Persian that are not part of the standard Arabic alphabet.
         *
         * Do **not** include ک (U+06A9) or ی (U+06CC): models often emit those in Arabic too.
         */
        private fun isPersianOnlyLetter(codePoint: Int): Boolean =
            codePoint == 0x067E || // پ
                codePoint == 0x0686 || // چ
                codePoint == 0x0698 || // ژ
                codePoint == 0x06AF // گ
    }
}
