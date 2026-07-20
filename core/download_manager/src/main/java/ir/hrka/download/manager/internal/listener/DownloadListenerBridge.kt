package ir.hrka.download.manager.internal.listener

import androidx.work.WorkInfo
import androidx.work.WorkManager
import ir.hrka.download.manager.error.DownloadError
import ir.hrka.download.manager.model.DownloadStatus
import ir.hrka.download.manager.api.DownloadListener
import ir.hrka.download.manager.internal.work.DownloadWorkProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Observes WorkManager progress for a download job and dispatches events to [DownloadListener].
 *
 * Bridges internal [DownloadWorkProgress] snapshots published by [ir.hrka.download.manager.internal.work.DownloadWorker]
 * to the public [DownloadListener] API. Duplicate terminal callbacks are suppressed.
 *
 * @property workManager WorkManager instance used to observe job progress.
 * @property listener Public callback receiver.
 * @property scope Coroutine scope used for observation; should use the main dispatcher.
 */
internal class DownloadListenerBridge(
    private val workManager: WorkManager,
    private val listener: DownloadListener,
    private val scope: CoroutineScope,
) {

    private var observationJob: Job? = null
    private var lastDispatchedStatus: DownloadStatus? = null
    private var hasDispatchedStart: Boolean = false

    /**
     * Starts observing [workId] and forwarding progress to [listener].
     *
     * Cancels any previous observation started by this bridge instance.
     *
     * @param workId WorkManager request id returned when the download was enqueued.
     */
    fun observe(workId: UUID) {
        observationJob?.cancel()
        observationJob = scope.launch {
            workManager.getWorkInfoByIdFlow(workId)
                .distinctUntilChanged { old, new ->
                    old?.state == new?.state &&
                        old?.progress == new?.progress
                }
                .collect { workInfo ->
                    val info = workInfo ?: return@collect
                    dispatch(info)

                    if (info.state.isFinished) {
                        observationJob?.cancel()
                        observationJob = null
                    }
                }
        }
    }

    /**
     * Cancels active WorkManager observation.
     */
    fun cancel() {
        observationJob?.cancel()
        observationJob = null
    }

    /**
     * Clears deduplication state before a new download run.
     */
    fun reset() {
        lastDispatchedStatus = null
        hasDispatchedStart = false
    }

    /**
     * Maps [WorkInfo] progress to [DownloadListener] callbacks.
     */
    private fun dispatch(workInfo: WorkInfo) {
        val progress = DownloadWorkProgress.fromWorkInfo(workInfo)

        when (progress.status) {
            DownloadStatus.Downloading -> {
                if (!hasDispatchedStart) {
                    listener.onStartDownload()
                    hasDispatchedStart = true
                }
                listener.onDownloading(
                    receivedBytes = progress.receivedBytes,
                    downloadRate = progress.downloadRate,
                    remainingTime = progress.remainingTimeMs,
                    progress = progress.progress,
                    currentPartIndex = progress.currentPartIndex,
                    totalParts = progress.totalParts,
                )
            }

            DownloadStatus.Paused -> {
                listener.onDownloadPaused(
                    receivedBytes = progress.receivedBytes,
                    downloadRate = progress.downloadRate,
                    remainingTime = progress.remainingTimeMs,
                    progress = progress.progress,
                    currentPartIndex = progress.currentPartIndex,
                    totalParts = progress.totalParts,
                )
            }

            DownloadStatus.Completed -> {
                if (lastDispatchedStatus != DownloadStatus.Completed) {
                    listener.onDownloadSuccess(progress.outputFilePath)
                }
            }

            DownloadStatus.Failed -> {
                if (lastDispatchedStatus != DownloadStatus.Failed) {
                    listener.onDownloadFailed(
                        DownloadError.fromSerialized(
                            code = progress.errorCode,
                            message = progress.errorMessage,
                        ),
                    )
                }
            }

            DownloadStatus.Canceled -> {
                if (lastDispatchedStatus != DownloadStatus.Canceled) {
                    listener.onDownloadCancelled()
                }
            }

            DownloadStatus.Pending,
            DownloadStatus.Merging,
            -> Unit
        }

        if (workInfo.state == WorkInfo.State.CANCELLED &&
            lastDispatchedStatus != DownloadStatus.Canceled
        ) {
            listener.onDownloadCancelled()
            lastDispatchedStatus = DownloadStatus.Canceled
            return
        }

        if (workInfo.state == WorkInfo.State.FAILED &&
            progress.status != DownloadStatus.Failed &&
            lastDispatchedStatus != DownloadStatus.Failed
        ) {
            listener.onDownloadFailed(
                DownloadError.fromSerialized(
                    code = progress.errorCode,
                    message = progress.errorMessage ?: "Download failed. Please try again.",
                ),
            )
            lastDispatchedStatus = DownloadStatus.Failed
            return
        }

        lastDispatchedStatus = progress.status
    }
}
