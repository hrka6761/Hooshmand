package ir.hrka.download.manager.api

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ir.hrka.download.manager.internal.listener.DownloadListenerBridge
import ir.hrka.download.manager.internal.work.DownloadWorkerControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Observes an in-flight download by its stable [downloadId] and forwards events to [DownloadListener].
 *
 * Use this to re-attach UI after the host process or ViewModel is recreated while WorkManager
 * continues the download in the background.
 */
class DownloadProgressObserver(
    context: Context,
    private val downloadId: String,
    private val listener: DownloadListener,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private var bridge: DownloadListenerBridge? = null
    private var observationJob: Job? = null

    /** Starts observing WorkManager progress for [downloadId]. */
    fun startObserving() {
        observationJob?.cancel()
        observationJob =
            scope.launch {
                workManager.getWorkInfosForUniqueWorkFlow(downloadId).collect { workInfos ->
                    if (workInfos.isEmpty()) {
                        stopObserving()
                        return@collect
                    }

                    val workInfo =
                        workInfos.lastOrNull { !it.state.isFinished }
                            ?: workInfos.lastOrNull()
                            ?: return@collect

                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.RUNNING,
                        -> attachBridge(workInfo.id)

                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.CANCELLED,
                        -> {
                            attachBridge(workInfo.id)
                            if (workInfo.state.isFinished) {
                                observationJob?.cancel()
                                observationJob = null
                            }
                        }

                        else -> Unit
                    }
                }
            }
    }

    /** Stops WorkManager observation without cancelling the download. */
    fun stopObserving() {
        bridge?.cancel()
        bridge = null
        observationJob?.cancel()
        observationJob = null
    }

    /**
     * Returns whether a download with [downloadId] is currently queued or running.
     */
    suspend fun isDownloadActive(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWorkFlow(downloadId).first()
        return workInfos.any { info ->
            info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING
        }
    }

    companion object {
        /** Cancels the download identified by [downloadId]. */
        fun cancelDownload(context: Context, downloadId: String) {
            DownloadWorkerControl.requestCancel(downloadId)
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(downloadId)
        }

        /** Pauses the download identified by [downloadId]. */
        fun pauseDownload(context: Context, downloadId: String) {
            DownloadWorkerControl.requestPause(downloadId)
        }

        /** Resumes a paused download identified by [downloadId]. */
        fun resumeDownload(context: Context, downloadId: String) {
            DownloadWorkerControl.requestResume(downloadId)
        }
    }

    private fun attachBridge(workId: java.util.UUID) {
        if (bridge == null) {
            bridge =
                DownloadListenerBridge(workManager, listener, scope).also {
                    it.reset()
                    it.observe(workId)
                }
        }
    }
}
