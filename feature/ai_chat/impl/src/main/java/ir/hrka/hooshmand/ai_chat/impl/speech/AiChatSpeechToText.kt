package ir.hrka.hooshmand.ai_chat.impl.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Result / error surface for speech-to-text.
 */
internal sealed interface AiChatListenResult {
    data object Started : AiChatListenResult

    data object Stopped : AiChatListenResult

    data object NotAvailable : AiChatListenResult

    data object Busy : AiChatListenResult

    enum class ErrorHint {
        Permission,
        NetworkOrOffline,
        NoMatch,
        Busy,
        Client,
        Audio,
        Server,
        Unknown,
    }
}

/**
 * Speech recognition for the chat input field.
 *
 * Uses [SpeechRecognizer] on the main thread. Language defaults to Persian (`fa-IR`) for this
 * app, Arabic when the system language is Arabic, and English when the system language is English.
 */
internal class AiChatSpeechToText(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var onPartialResult: ((String) -> Unit)? = null
    private var onFinalResult: ((String) -> Unit)? = null
    private var onError: ((AiChatListenResult.ErrorHint) -> Unit)? = null

    /** Whether this device exposes a speech recognition service. */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    /**
     * Starts listening and delivers transcripts through [onPartial] / [onFinal].
     *
     * Cancels any previous session first so restarting with existing input text is safe.
     */
    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (AiChatListenResult.ErrorHint) -> Unit,
    ): AiChatListenResult {
        if (!isAvailable()) return AiChatListenResult.NotAvailable

        this.onPartialResult = onPartial
        this.onFinalResult = onFinal
        this.onError = onError

        val intent = buildRecognizerIntent()

        // Always reset on the main thread before starting a new session.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            restartListening(intent, onError)
        } else {
            mainHandler.post { restartListening(intent, onError) }
        }
        return AiChatListenResult.Started
    }

    private fun restartListening(
        intent: Intent,
        onError: (AiChatListenResult.ErrorHint) -> Unit,
    ) {
        try {
            // Cancel a previous session without destroying — avoids ERROR_CLIENT / BUSY
            // when the user taps mic again while the field already has text.
            runCatching { recognizer?.cancel() }
            _isListening.value = false

            if (recognizer == null) {
                recognizer =
                    SpeechRecognizer.createSpeechRecognizer(appContext).also { engine ->
                        engine.setRecognitionListener(createListener())
                    }
            }

            _isListening.value = true
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            _isListening.value = false
            onError(AiChatListenResult.ErrorHint.Client)
        }
    }

    /** Stops the current recognition session without destroying the recognizer. */
    fun stop(): AiChatListenResult {
        if (!_isListening.value) return AiChatListenResult.Stopped
        runOnMain {
            runCatching { recognizer?.stopListening() }
        }
        _isListening.value = false
        return AiChatListenResult.Stopped
    }

    /** Cancels listening and releases the [SpeechRecognizer]. */
    fun destroy() {
        _isListening.value = false
        onPartialResult = null
        onFinalResult = null
        onError = null
        runOnMain {
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    private fun createListener(): RecognitionListener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                // cancel() before a fresh startListening can emit ERROR_CLIENT; ignore it
                // when we are already starting/ready for a new utterance.
                if (error == SpeechRecognizer.ERROR_CLIENT && _isListening.value) {
                    return
                }
                _isListening.value = false
                val hint = mapError(error)
                // Swallow benign end-of-session errors.
                if (
                    hint == AiChatListenResult.ErrorHint.NoMatch ||
                    hint == AiChatListenResult.ErrorHint.Client
                ) {
                    return
                }
                onError?.invoke(hint)
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val text = firstResult(results) ?: return
                onFinalResult?.invoke(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = firstResult(partialResults) ?: return
                onPartialResult?.invoke(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

    private fun buildRecognizerIntent(): Intent {
        val locale = recognitionLocale()
        // BCP-47 tags work more reliably with Google / OEM engines than bare language codes.
        val languageTag =
            when (locale.language) {
                "fa" -> "fa-IR"
                "ar" -> "ar"
                else -> "en-US"
            }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            // Do not force EXTRA_PREFER_OFFLINE: an English-only offline pack would
            // transcribe Persian/Arabic speech as English. Online or matching offline packs
            // are used according to what the device engine supports for [languageTag].
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    companion object {
        /**
         * Recognition language for voice input.
         *
         * Hooshmand defaults to Persian. Arabic is used when the system language is Arabic.
         * Draft text is not used for language selection (the field is cleared on each mic tap).
         */
        fun recognitionLocale(): Locale {
            val system = Locale.getDefault()
            return if (system.language == "ar") {
                Locale.forLanguageTag("ar")
            } else {
                Locale.forLanguageTag("fa-IR")
            }
        }

        private fun firstResult(bundle: Bundle?): String? {
            val matches =
                bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return null
            return matches.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        }

        private fun mapError(error: Int): AiChatListenResult.ErrorHint =
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    AiChatListenResult.ErrorHint.Permission

                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                -> AiChatListenResult.ErrorHint.NetworkOrOffline

                SpeechRecognizer.ERROR_NO_MATCH -> AiChatListenResult.ErrorHint.NoMatch
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> AiChatListenResult.ErrorHint.Busy
                SpeechRecognizer.ERROR_CLIENT -> AiChatListenResult.ErrorHint.Client
                SpeechRecognizer.ERROR_AUDIO -> AiChatListenResult.ErrorHint.Audio
                SpeechRecognizer.ERROR_SERVER,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> AiChatListenResult.ErrorHint.Server

                else -> AiChatListenResult.ErrorHint.Unknown
            }
    }
}
