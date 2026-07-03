package ir.hrka.download.manager.internal.work

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-process control flags shared between [DownloadWorker] and
 * [ir.hrka.download.manager.internal.receiver.DownloadActionReceiver].
 *
 * WorkManager workers and notification action receivers run in the same app process, so
 * thread-safe in-memory flags are sufficient for pause, resume, and cancel coordination.
 *
 * Call [register] when a worker starts and [unregister] when it finishes to avoid leaking entries.
 */
internal object DownloadWorkerControl {

    private val pauseRequested = ConcurrentHashMap<String, AtomicBoolean>()
    private val cancelRequested = ConcurrentHashMap<String, AtomicBoolean>()

    /**
     * Initializes control flags for [downloadId].
     *
     * Both pause and cancel start cleared (`false`).
     */
    fun register(downloadId: String) {
        pauseRequested.getOrPut(downloadId) { AtomicBoolean(false) }
        cancelRequested.getOrPut(downloadId) { AtomicBoolean(false) }
    }

    /**
     * Removes control state for [downloadId] when the worker completes.
     */
    fun unregister(downloadId: String) {
        pauseRequested.remove(downloadId)
        cancelRequested.remove(downloadId)
    }

    /**
     * Requests a cooperative pause for the active download identified by [downloadId].
     */
    fun requestPause(downloadId: String) {
        pauseRequested.getOrPut(downloadId) { AtomicBoolean(false) }.set(true)
    }

    /**
     * Clears a previously requested pause so the worker can continue.
     */
    fun requestResume(downloadId: String) {
        pauseRequested[downloadId]?.set(false)
    }

    /**
     * Requests cancellation and clears any active pause request.
     */
    fun requestCancel(downloadId: String) {
        cancelRequested.getOrPut(downloadId) { AtomicBoolean(false) }.set(true)
        pauseRequested[downloadId]?.set(false)
    }

    /** Returns `true` when pause was requested and not yet cleared by [requestResume]. */
    fun isPauseRequested(downloadId: String): Boolean =
        pauseRequested[downloadId]?.get() == true

    /** Returns `true` when cancel was requested via [requestCancel]. */
    fun isCancelRequested(downloadId: String): Boolean =
        cancelRequested[downloadId]?.get() == true

    /**
     * Returns `true` when the active transfer should stop cooperatively.
     *
     * Used by [ir.hrka.download.manager.core.OkHttpDownloader] during byte streaming.
     */
    fun shouldStop(downloadId: String): Boolean =
        isPauseRequested(downloadId) || isCancelRequested(downloadId)
}
