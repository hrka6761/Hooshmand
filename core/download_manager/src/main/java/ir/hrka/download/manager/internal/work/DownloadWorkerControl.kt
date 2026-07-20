package ir.hrka.download.manager.internal.work

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-process control flags shared between [DownloadWorker] and
 * [ir.hrka.download.manager.api.DownloadActionReceiver].
 *
 * WorkManager workers and notification action receivers run in the same app process, so
 * thread-safe in-memory flags are sufficient for pause, resume, and cancel coordination.
 *
 * Call [register] when a worker starts and [unregister] when it finishes to avoid leaking entries.
 */
internal object DownloadWorkerControl {

    private val pauseRequested = ConcurrentHashMap<String, AtomicBoolean>()
    private val cancelRequested = ConcurrentHashMap<String, AtomicBoolean>()
    private val wakeSignals = ConcurrentHashMap<String, Channel<Unit>>()

    private fun pauseFlag(downloadId: String): AtomicBoolean =
        pauseRequested.getOrPut(downloadId) { AtomicBoolean(false) }

    private fun cancelFlag(downloadId: String): AtomicBoolean =
        cancelRequested.getOrPut(downloadId) { AtomicBoolean(false) }

    private fun wakeChannel(downloadId: String): Channel<Unit> =
        wakeSignals.getOrPut(downloadId) { Channel(Channel.CONFLATED) }

    private fun signalWaiters(downloadId: String) {
        wakeChannel(downloadId).trySend(Unit)
    }

    /**
     * Ensures control state exists for [downloadId] without clearing user requests.
     */
    fun register(downloadId: String) {
        pauseFlag(downloadId)
        cancelFlag(downloadId)
        wakeChannel(downloadId)
    }

    /**
     * Clears pause/cancel state when a brand-new download job is enqueued.
     */
    fun resetForNewDownload(downloadId: String) {
        pauseFlag(downloadId).set(false)
        cancelFlag(downloadId).set(false)
        signalWaiters(downloadId)
    }

    /**
     * Removes control state for [downloadId] when the worker completes.
     */
    fun unregister(downloadId: String) {
        pauseRequested.remove(downloadId)
        cancelRequested.remove(downloadId)
        wakeSignals.remove(downloadId)?.close()
        ActiveDownloadTransferRegistry.unregister(downloadId)
    }

    /**
     * Requests a cooperative pause for the active download identified by [downloadId].
     *
     * Does not clear [isCancelRequested]; a concurrent stop request must remain observable.
     */
    fun requestPause(downloadId: String) {
        pauseFlag(downloadId).set(true)
        ActiveDownloadTransferRegistry.interrupt(downloadId)
        signalWaiters(downloadId)
    }

    /**
     * Clears a previously requested pause so the worker can continue.
     *
     * Does not clear [isCancelRequested]; a concurrent stop request must still win.
     */
    fun requestResume(downloadId: String) {
        pauseFlag(downloadId).set(false)
        signalWaiters(downloadId)
    }

    /**
     * Requests cancellation without clearing an active pause request.
     *
     * The worker must observe the cancel flag while still paused; clearing pause here would
     * cause [DownloadWorker.waitForContinueOrCancel] to resume the transfer instead of stopping.
     */
    fun requestCancel(downloadId: String) {
        cancelFlag(downloadId).set(true)
        ActiveDownloadTransferRegistry.interrupt(downloadId)
        signalWaiters(downloadId)
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
     * Used by [ir.hrka.download.manager.internal.transfer.OkHttpDownloader] during byte streaming.
     */
    fun shouldStop(downloadId: String): Boolean =
        isPauseRequested(downloadId) || isCancelRequested(downloadId)

    /**
     * Suspends until a control signal arrives or [timeoutMs] elapses.
     */
    suspend fun awaitControlSignal(downloadId: String, timeoutMs: Long) {
        withTimeoutOrNull(timeoutMs) {
            wakeChannel(downloadId).receive()
        }
    }
}
