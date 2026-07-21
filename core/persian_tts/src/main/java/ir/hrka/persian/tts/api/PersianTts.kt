package ir.hrka.persian.tts.api

import kotlinx.coroutines.flow.StateFlow

/**
 * Offline Persian text-to-speech engine.
 *
 * This API ships a single Piper Persian (`fa_IR`) voice backed by sherpa-onnx.
 * It is intentionally **not** multilingual — do not use it for Arabic, English,
 * or other languages (use Android [android.speech.tts.TextToSpeech] or another
 * voice model for those).
 *
 * ## Lifecycle
 *
 * 1. Create an instance via [PersianTtsFactory.create].
 * 2. Observe [isReady] (or handle [PersianTtsResult.EngineNotReady]) before
 *    expecting speech; model load runs asynchronously on a background thread.
 * 3. Call [speakOrStop] / [stop] as needed.
 * 4. Call [shutdown] when the host screen or process no longer needs TTS.
 *
 * Implementations are thread-safe for the public methods above.
 *
 * @see PersianTtsFactory
 * @see PersianTtsResult
 */
interface PersianTts {
    /**
     * Emits `true` after the ONNX model and audio path have been loaded
     * successfully; `false` while initializing, after a failed init, or after
     * [shutdown].
     */
    val isReady: StateFlow<Boolean>

    /**
     * Emits `true` while an utterance is being synthesized or played back.
     */
    val isSpeaking: StateFlow<Boolean>

    /**
     * Id of the utterance currently speaking, or `null` when idle.
     *
     * Matches the [utteranceId] last passed to [speakOrStop] that started
     * playback.
     */
    val speakingUtteranceId: StateFlow<String?>

    /**
     * Speaks [text] for [utteranceId], or stops if the same utterance is already
     * playing (toggle).
     *
     * Any previous utterance is stopped first (flush). Input may contain light
     * markdown; implementations normalize it to plain prose before synthesis.
     *
     * @param utteranceId Caller-defined id used for toggle / UI correlation
     *   (for example a chat message id).
     * @param text Text to speak; blank text after normalization yields
     *   [PersianTtsResult.EmptyText].
     * @return Outcome of the speak/stop request; never throws for normal
     *   control flow.
     */
    fun speakOrStop(
        utteranceId: String,
        text: String,
    ): PersianTtsResult

    /**
     * Stops the current utterance if any.
     *
     * Safe to call when idle. Does not release the engine; use [shutdown] for
     * that.
     */
    fun stop()

    /**
     * Releases native resources (ONNX engine, [android.media.AudioTrack], worker).
     *
     * The instance must not be used after this call. [isReady] becomes `false`.
     */
    fun shutdown()
}
