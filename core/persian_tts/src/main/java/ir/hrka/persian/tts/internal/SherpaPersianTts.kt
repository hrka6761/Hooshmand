package ir.hrka.persian.tts.internal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import ir.hrka.persian.tts.api.PersianTts
import ir.hrka.persian.tts.api.PersianTtsResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sherpa-ONNX / Piper implementation of [PersianTts].
 *
 * Loads `fa_IR-gyro-medium` (int8) via [PersianTtsAssetInstaller], synthesizes
 * with `OfflineTts.generate`, and plays PCM float samples through a streamed
 * [AudioTrack].
 *
 * ## Latency
 *
 * Full-utterance `generate()` would block until **all** audio is ready. Long
 * replies are therefore split into short segments: each segment is synthesized
 * and written immediately so playback can start after the first segment.
 *
 * ## Threading
 *
 * - Construction kicks off async init on a dedicated single-thread worker.
 * - [speakOrStop] enqueues synthesis/playback on that worker.
 * - [stop] / [shutdown] interrupt playback and may replace the worker executor.
 *
 * ## JNI note
 *
 * Prefer `generate()` over `generateWithCallback()`. JNI callbacks into Kotlin
 * lambdas can crash on modern AGP/D8 with
 * `NoSuchMethodError: invoke([F)Ljava/lang/Integer;`.
 *
 * @param context Any context; only the application context is retained.
 */
internal class SherpaPersianTts(
    context: Context,
) : PersianTts {
    /** Application context used for asset install and lifetime. */
    private val appContext = context.applicationContext

    /** Set once by [shutdown]; blocks further speak/init work. */
    private val shutDown = AtomicBoolean(false)

    /** Set while stop is in progress so the worker aborts write loops. */
    private val stopRequested = AtomicBoolean(false)

    private val _isReady = MutableStateFlow(false)

    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)

    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingUtteranceId = MutableStateFlow<String?>(null)

    override val speakingUtteranceId: StateFlow<String?> = _speakingUtteranceId.asStateFlow()

    /** Live sherpa engine, or `null` before ready / after release. */
    private val ttsRef = AtomicReference<OfflineTts?>(null)

    /** Live playback track matching the engine sample rate. */
    private val trackRef = AtomicReference<AudioTrack?>(null)

    /** Guards [worker] replacement during stop/shutdown. */
    private val workerLock = Any()

    /** Single-thread executor for init, synthesize, and audio write. */
    private var worker: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "persian-tts-worker").apply { isDaemon = true }
        }

    init {
        worker.execute {
            try {
                if (shutDown.get()) return@execute
                val paths = PersianTtsAssetInstaller.ensureInstalled(appContext)
                if (shutDown.get()) return@execute

                val config =
                    OfflineTtsConfig(
                        model =
                            OfflineTtsModelConfig(
                                vits =
                                    OfflineTtsVitsModelConfig(
                                        model = paths.modelPath,
                                        tokens = paths.tokensPath,
                                        dataDir = paths.espeakDataDir,
                                        lexicon = "",
                                    ),
                                numThreads = 2,
                                debug = false,
                                provider = "cpu",
                            ),
                        // Batch size for long text (not a hard sentence cap).
                        maxNumSentences = 2,
                    )

                val engine = OfflineTts(assetManager = null, config = config)
                if (engine.numSpeakers() <= 0) {
                    runCatching { engine.release() }
                    throw IllegalStateException("Persian TTS model reported zero speakers")
                }

                val track = createAudioTrack(engine.sampleRate())
                ttsRef.set(engine)
                trackRef.set(track)
                _isReady.value = true
                Log.i(TAG, "Persian TTS ready (sampleRate=${engine.sampleRate()})")
            } catch (t: Throwable) {
                Log.e(TAG, "Persian TTS init failed", t)
                releaseEngineQuietly()
                _isReady.value = false
            }
        }
    }

    override fun speakOrStop(
        utteranceId: String,
        text: String,
    ): PersianTtsResult {
        if (shutDown.get()) {
            return PersianTtsResult.EngineNotReady
        }

        if (_speakingUtteranceId.value == utteranceId && _isSpeaking.value) {
            stop()
            return PersianTtsResult.Ok
        }

        val plain = normalizeText(text)
        if (plain.isBlank()) {
            return PersianTtsResult.EmptyText
        }

        val engine = ttsRef.get()
        val track = trackRef.get()
        if (!_isReady.value || engine == null || track == null) {
            return PersianTtsResult.EngineNotReady
        }

        stopInternal(restartWorker = true)

        stopRequested.set(false)
        _speakingUtteranceId.value = utteranceId
        _isSpeaking.value = true

        try {
            worker.execute {
                speakOnWorker(
                    engine = engine,
                    track = track,
                    utteranceId = utteranceId,
                    text = plain,
                )
            }
        } catch (t: Throwable) {
            clearSpeakingState()
            return PersianTtsResult.SpeakFailed(
                message = t.message ?: "Failed to enqueue Persian speech",
                cause = t,
            )
        }

        return PersianTtsResult.Ok
    }

    override fun stop() {
        stopInternal(restartWorker = true)
    }

    override fun shutdown() {
        if (!shutDown.compareAndSet(false, true)) return
        _isReady.value = false
        stopInternal(restartWorker = false)
        synchronized(workerLock) {
            runCatching { worker.shutdownNow() }
        }
        releaseEngineQuietly()
    }

    /**
     * Runs synthesis and blocking playback on the worker thread.
     *
     * Long text is split into short segments. Each segment is generated with
     * `OfflineTts.generate` and written to [track] before the next segment is
     * synthesized, so the listener hears audio after the first segment instead
     * of waiting for the entire utterance.
     *
     * @param engine Loaded sherpa OfflineTts instance.
     * @param track Initialized [AudioTrack] at the model sample rate.
     * @param utteranceId Id for log correlation only (state already set).
     * @param text Already-normalized plain text.
     */
    private fun speakOnWorker(
        engine: OfflineTts,
        track: AudioTrack,
        utteranceId: String,
        text: String,
    ) {
        if (shutDown.get() || stopRequested.get()) {
            clearSpeakingState()
            return
        }

        try {
            setSpeaking(true)

            val segments = splitForStreamingSpeech(text)
            if (segments.isEmpty()) {
                Log.w(TAG, "Persian TTS had no speakable segments for utterance=$utteranceId")
                return
            }

            var wroteAny = false
            for (segment in segments) {
                if (shutDown.get() || stopRequested.get()) {
                    return
                }

                // generate() (not generateWithCallback): Kotlin/JNI lambdas crash
                // on modern AGP/D8 with NoSuchMethodError for invoke([F)Integer.
                val samples = engine.generate(text = segment, sid = 0, speed = SPEECH_SPEED).samples
                if (samples.isEmpty()) {
                    Log.w(TAG, "Empty audio for segment of utterance=$utteranceId")
                    continue
                }
                if (shutDown.get() || stopRequested.get()) {
                    return
                }

                ensurePlaying(track)
                writeSamples(track, samples)
                wroteAny = true
            }

            if (!wroteAny) {
                Log.w(TAG, "Persian TTS generated no audio for utterance=$utteranceId")
            }
        } catch (t: Throwable) {
            if (!stopRequested.get() && !shutDown.get()) {
                Log.e(TAG, "Persian speech synthesis failed", t)
            }
        } finally {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.flush()
            }
            clearSpeakingState()
        }
    }

    /**
     * Splits [text] into short speakable segments for lower time-to-first-audio.
     *
     * Prefers sentence-ending punctuation (including Persian `؟` / `۔`), then
     * further splits oversized spans on whitespace so a single run-on sentence
     * cannot recreate the full-utterance latency problem.
     *
     * @param text Normalized plain prose.
     * @return Non-blank segments in order; empty if [text] is blank.
     */
    private fun splitForStreamingSpeech(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= MAX_SEGMENT_CHARS) return listOf(trimmed)

        val bySentence = SENTENCE_SPLIT_REGEX.split(trimmed).map { it.trim() }.filter { it.isNotEmpty() }
        if (bySentence.isEmpty()) {
            return chunkByWords(trimmed, MAX_SEGMENT_CHARS)
        }

        val segments = ArrayList<String>(bySentence.size)
        for (sentence in bySentence) {
            if (sentence.length <= MAX_SEGMENT_CHARS) {
                segments += sentence
            } else {
                segments += chunkByWords(sentence, MAX_SEGMENT_CHARS)
            }
        }
        return segments
    }

    /**
     * Packs whitespace-separated tokens into chunks of at most [maxChars].
     *
     * @param text Source span (already trimmed).
     * @param maxChars Soft upper bound on chunk length.
     * @return One or more chunks covering [text] in order.
     */
    private fun chunkByWords(
        text: String,
        maxChars: Int,
    ): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val chunks = ArrayList<String>()
        val current = StringBuilder()
        for (word in words) {
            if (current.isEmpty()) {
                current.append(word)
                continue
            }
            if (current.length + 1 + word.length <= maxChars) {
                current.append(' ').append(word)
            } else {
                chunks += current.toString()
                current.clear()
                current.append(word)
            }
        }
        if (current.isNotEmpty()) {
            chunks += current.toString()
        }
        return chunks
    }

    /**
     * Writes PCM float samples in chunks so [stop] can interrupt long playback
     * sooner than a single giant `write`.
     *
     * @param track Destination track (must already be playable).
     * @param samples Mono PCM floats in `[-1, 1]`.
     */
    private fun writeSamples(
        track: AudioTrack,
        samples: FloatArray,
    ) {
        var offset = 0
        val chunkSize = 8_192
        while (offset < samples.size) {
            if (shutDown.get() || stopRequested.get()) {
                return
            }
            val length = minOf(chunkSize, samples.size - offset)
            ensurePlaying(track)
            val written =
                track.write(
                    samples,
                    offset,
                    length,
                    AudioTrack.WRITE_BLOCKING,
                )
            if (written < 0) {
                Log.e(TAG, "AudioTrack.write failed: $written")
                return
            }
            if (written == 0) {
                // Avoid a tight spin if the track is not accepting data.
                try {
                    Thread.sleep(5L)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
                continue
            }
            offset += written
        }
    }

    /**
     * Requests stop, pauses/flushes the track, and optionally recreates the
     * worker so a cancelled task cannot block the next utterance.
     *
     * @param restartWorker When `true`, shuts down the current executor and
     *   starts a fresh one (speak/stop). When `false`, only shuts down (used by
     *   [shutdown]).
     */
    private fun stopInternal(restartWorker: Boolean) {
        stopRequested.set(true)
        clearSpeakingState()

        runCatching {
            trackRef.get()?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
                track.flush()
                runCatching { track.stop() }
            }
        }

        synchronized(workerLock) {
            runCatching { worker.shutdownNow() }
            if (restartWorker && !shutDown.get()) {
                worker =
                    Executors.newSingleThreadExecutor { runnable ->
                        Thread(runnable, "persian-tts-worker").apply { isDaemon = true }
                    }
            }
        }

        stopRequested.set(false)
    }

    /**
     * Releases the OfflineTts engine and [AudioTrack] if present, clearing the
     * atomic refs. Errors during release are swallowed.
     */
    private fun releaseEngineQuietly() {
        ttsRef.getAndSet(null)?.let { engine ->
            runCatching { engine.release() }
        }
        trackRef.getAndSet(null)?.let { track ->
            runCatching {
                track.pause()
                track.flush()
                track.stop()
            }
            runCatching { track.release() }
        }
    }

    /**
     * Ensures [track] is in [AudioTrack.PLAYSTATE_PLAYING].
     *
     * @param track Track to start if paused/stopped.
     */
    private fun ensurePlaying(track: AudioTrack) {
        if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
        }
    }

    /**
     * Updates [isSpeaking] without touching [speakingUtteranceId].
     *
     * @param speaking New speaking flag.
     */
    private fun setSpeaking(speaking: Boolean) {
        _isSpeaking.value = speaking
    }

    /** Clears both speaking flag and current utterance id. */
    private fun clearSpeakingState() {
        _isSpeaking.value = false
        _speakingUtteranceId.value = null
    }

    /**
     * Creates a streamed mono PCM-float [AudioTrack] for [sampleRate].
     *
     * @param sampleRate Model output sample rate in Hz.
     * @return An initialized track ready for [AudioTrack.play].
     * @throws IllegalStateException if buffer size is invalid or init fails.
     */
    private fun createAudioTrack(sampleRate: Int): AudioTrack {
        val minBuffer =
            AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            )
        if (minBuffer <= 0) {
            throw IllegalStateException("Invalid AudioTrack buffer size: $minBuffer")
        }

        val bufferSize = minBuffer.coerceAtLeast(sampleRate / 2) * 4

        @Suppress("DEPRECATION")
        val track =
            AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE,
            )

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { track.release() }
            throw IllegalStateException("AudioTrack failed to initialize")
        }
        return track
    }

    /**
     * Strips common markdown markers so the synthesizer receives plain prose.
     *
     * @param text Raw user/model text (may include fenced code, links, emphasis).
     * @return Collapsed plain text, or blank if nothing remain.
     */
    private fun normalizeText(text: String): String =
        text
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

    private companion object {
        /** Logcat tag. */
        const val TAG = "SherpaPersianTts"

        /**
         * Piper generation speed multiplier.
         *
         * Values below `1.0f` slow narration slightly for clearer Persian speech.
         */
        const val SPEECH_SPEED = 0.85f

        /**
         * Soft max characters per synthesis segment.
         *
         * Small enough that the first `generate()` returns quickly; large enough
         * to avoid choppy per-word synthesis.
         */
        const val MAX_SEGMENT_CHARS = 140

        /**
         * Splits after sentence-ending punctuation (Latin + Persian) or newlines,
         * keeping the terminator with the preceding segment.
         */
        val SENTENCE_SPLIT_REGEX = Regex("""(?<=[.!?؟۔])\s+|\n+""")
    }
}
