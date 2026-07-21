package ir.hrka.persian.tts.api

import android.content.Context
import ir.hrka.persian.tts.internal.SherpaPersianTts

/**
 * Factory for [PersianTts] instances.
 *
 * Each call creates an independent engine that loads the bundled Persian Piper
 * model (`fa_IR-gyro-medium`) from module assets into app-private storage, then
 * initializes sherpa-onnx on a background worker thread.
 *
 * ## Example
 *
 * ```kotlin
 * val tts = PersianTtsFactory.create(context)
 * // Wait until ready, then:
 * when (tts.speakOrStop("msg-1", "سلام")) {
 *     PersianTtsResult.Ok -> Unit
 *     PersianTtsResult.EngineNotReady -> { /* still loading */ }
 *     PersianTtsResult.EmptyText -> Unit
 *     is PersianTtsResult.SpeakFailed -> { /* log error.message */ }
 * }
 * // Later:
 * tts.shutdown()
 * ```
 */
object PersianTtsFactory {
    /**
     * Creates a new offline Persian TTS engine.
     *
     * Initialization runs asynchronously. Observe [PersianTts.isReady] or handle
     * [PersianTtsResult.EngineNotReady] before expecting speech.
     *
     * @param context Any context; the application context is retained internally.
     * @return A fresh [PersianTts] bound to [context]'s application context.
     */
    fun create(context: Context): PersianTts =
        SherpaPersianTts(context.applicationContext)
}
