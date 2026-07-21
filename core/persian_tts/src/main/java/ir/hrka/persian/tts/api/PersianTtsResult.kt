package ir.hrka.persian.tts.api

/**
 * Result of a [PersianTts.speakOrStop] request.
 *
 * Callers should treat this as a sealed control-flow type rather than relying on
 * exceptions for ordinary outcomes (not ready, empty text, toggle-stop).
 */
sealed interface PersianTtsResult {
    /**
     * Playback was started successfully, or the same [utteranceId] was already
     * speaking and was toggled off via [PersianTts.speakOrStop].
     */
    data object Ok : PersianTtsResult

    /**
     * Engine is still initializing, failed to init, or was already shut down.
     *
     * Retry after [PersianTts.isReady] becomes `true`, or create a new instance
     * via [PersianTtsFactory] if the previous one was shut down.
     */
    data object EngineNotReady : PersianTtsResult

    /**
     * Input text was blank after markdown / whitespace normalization.
     */
    data object EmptyText : PersianTtsResult

    /**
     * Synthesis enqueue or audio playback failed unexpectedly.
     *
     * @property message Short human-readable description suitable for logs.
     * @property cause Optional underlying throwable when available.
     */
    data class SpeakFailed(
        val message: String,
        val cause: Throwable? = null,
    ) : PersianTtsResult
}
