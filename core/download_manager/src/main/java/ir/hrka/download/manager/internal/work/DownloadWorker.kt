package ir.hrka.download.manager.internal.work

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import ir.hrka.download.manager.error.DownloadError
import ir.hrka.download.manager.internal.notification.DownloadNotificationHelper
import ir.hrka.download.manager.internal.storage.FileProviderFactory
import ir.hrka.download.manager.internal.transfer.OkHttpDownloader
import ir.hrka.download.manager.internal.transfer.OkHttpListener
import ir.hrka.download.manager.model.DownloadStatus
import ir.hrka.download.manager.model.DownloadType
import ir.hrka.download.manager.model.FileCreationMode
import ir.hrka.download.manager.model.SingleItemDownloadData
import java.io.File
import java.io.IOException

/**
 * WorkManager worker that performs background downloads as a foreground service.
 *
 * Reads [DownloadWorkInput] from [inputData], publishes [DownloadWorkProgress] via
 * [setProgress], and keeps a foreground notification in sync through
 * [DownloadNotificationHelper]. Pause, resume, and stop requests are coordinated through
 * [DownloadWorkerControl] and [ir.hrka.download.manager.api.DownloadActionReceiver].
 *
 * Supports:
 * - [DownloadType.SingleFile] — one URL written to a single output file with resume support.
 * - [DownloadType.MultiParts] — ordered parts appended sequentially into one output file.
 *
 * @see DownloadWorkInput
 * @see DownloadWorkProgress
 * @see DownloadWorkerControl
 */
internal class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    /**
     * Entry point invoked by WorkManager.
     *
     * Deserializes input, registers control flags, runs the download loop, and maps the
     * terminal [DownloadWorkProgress.status] to a WorkManager [Result].
     *
     * @return [Result.success] on completion, [Result.failure] on cancel/failure,
     * [Result.retry] for unexpected non-terminal states.
     */
    override suspend fun doWork(): Result {
        val input =
            try {
                DownloadWorkInput.fromWorkData(inputData)
            } catch (error: Exception) {
                // Cannot publish typed progress without a valid download id / notification config.
                return Result.failure()
            }

        DownloadWorkerControl.register(input.downloadId)

        val notificationHelper = DownloadNotificationHelper.fromInput(applicationContext, input)
        val totalParts =
            when (input.downloadType) {
                DownloadType.SingleFile -> 1
                DownloadType.MultiParts -> input.parts.size.coerceAtLeast(1)
            }
        var lastProgress = DownloadWorkProgress.pending(input.fileSize, totalParts)

        try {
            publishProgress(notificationHelper, lastProgress)

            // Worker retries / process death must not wipe partial bytes written with Overwrite.
            val effectiveCreationMode =
                if (runAttemptCount > 0 && input.creationMode == FileCreationMode.Overwrite) {
                    FileCreationMode.Append
                } else {
                    input.creationMode
                }

            val fileProvider =
                FileProviderFactory.create(
                    context = applicationContext,
                    input = input,
                    creationMode = effectiveCreationMode,
                )
            val outputFile = fileProvider.provide()
            val downloader = OkHttpDownloader(fileProvider)

            lastProgress =
                when (input.downloadType) {
                    DownloadType.SingleFile ->
                        downloadSingleFile(
                            input = input,
                            downloader = downloader,
                            outputFile = outputFile,
                            notificationHelper = notificationHelper,
                            downloadId = input.downloadId,
                            initialProgress = lastProgress,
                        )

                    DownloadType.MultiParts ->
                        downloadMultiParts(
                            input = input,
                            downloader = downloader,
                            outputFile = outputFile,
                            notificationHelper = notificationHelper,
                            downloadId = input.downloadId,
                            initialProgress = lastProgress,
                        )
                }

            return when (lastProgress.status) {
                DownloadStatus.Completed -> Result.success()
                DownloadStatus.Canceled -> Result.failure()
                DownloadStatus.Failed -> Result.failure()
                else -> Result.retry()
            }
        } catch (error: Exception) {
            val downloadError = DownloadError.fromThrowable(error)
            val failed =
                DownloadWorkProgress.failed(
                    error = downloadError,
                    lastKnown = lastProgress,
                )
            runCatching { publishProgress(notificationHelper, failed) }
            return Result.failure()
        } finally {
            DownloadWorkerControl.unregister(input.downloadId)
        }
    }

    /**
     * Downloads a single URL into [outputFile], honoring pause/cancel and resume from
     * the existing file length.
     */
    private suspend fun downloadSingleFile(
        input: DownloadWorkInput,
        downloader: OkHttpDownloader,
        outputFile: File,
        notificationHelper: DownloadNotificationHelper,
        downloadId: String,
        initialProgress: DownloadWorkProgress,
    ): DownloadWorkProgress {
        var lastProgress = initialProgress
        val totalBytes = input.fileSize

        while (true) {
            when (waitForContinueOrCancel(downloadId, lastProgress, notificationHelper)) {
                ControlAction.Cancel -> return DownloadWorkProgress.canceled(lastProgress)
                ControlAction.Continue -> Unit
            }

            val sourceOffset = outputFile.takeIf { it.exists() }?.length() ?: 0L
            if (totalBytes != null && sourceOffset >= totalBytes) {
                val completed = DownloadWorkProgress.completed(outputFile.absolutePath, totalBytes)
                publishProgress(notificationHelper, completed)
                return completed
            }

            lastProgress = syncProgressFromFile(
                outputFile = outputFile,
                lastProgress = lastProgress,
                totalBytes = totalBytes,
                totalParts = 1,
                currentPartIndex = 0,
                allParts = emptyList(),
            )
            publishProgress(notificationHelper, lastProgress)

            val outcome = executeSegment(
                downloader = downloader,
                outputFile = outputFile,
                url = input.url.orEmpty(),
                headers = input.headers,
                expectedSegmentBytes = totalBytes ?: 0L,
                appendToOutput = sourceOffset > 0L,
                sourceOffset = sourceOffset,
                overallReceivedBytes = 0L,
                overallTotalBytes = totalBytes,
                allParts = emptyList(),
                downloadId = downloadId,
                notificationHelper = notificationHelper,
                totalParts = 1,
                currentPartIndex = 0,
                currentPartTotalBytes = totalBytes,
            )

            lastProgress = outcome.progress ?: lastProgress
            when (outcome.type) {
                SegmentOutcomeType.Completed -> {
                    val completed = DownloadWorkProgress.completed(
                        outputFilePath = outputFile.absolutePath,
                        totalBytes = totalBytes ?: outputFile.length(),
                    )
                    publishProgress(notificationHelper, completed)
                    return completed
                }

                SegmentOutcomeType.Stopped -> continue
                SegmentOutcomeType.Failed -> {
                    val downloadError =
                        outcome.error?.let(DownloadError::fromThrowable)
                            ?: DownloadError.Unknown()
                    val failed =
                        DownloadWorkProgress.failed(
                            error = downloadError,
                            lastKnown = lastProgress,
                        )
                    publishProgress(notificationHelper, failed)
                    return failed
                }
            }
        }
    }

    /**
     * Downloads multipart URLs in order, appending each part into [outputFile].
     *
     * Resume position is derived from the current output file length and known part sizes.
     */
    private suspend fun downloadMultiParts(
        input: DownloadWorkInput,
        downloader: OkHttpDownloader,
        outputFile: File,
        notificationHelper: DownloadNotificationHelper,
        downloadId: String,
        initialProgress: DownloadWorkProgress,
    ): DownloadWorkProgress {
        var lastProgress = initialProgress
        val parts = input.parts
        val totalBytes = input.fileSize
        var resume = resolveMultipartResumeState(outputFile.length(), parts)

        if (resume.partIndex >= parts.size) {
            val completed = DownloadWorkProgress.completed(
                outputFilePath = outputFile.absolutePath,
                totalBytes = totalBytes ?: outputFile.length(),
            )
            publishProgress(notificationHelper, completed)
            return completed
        }

        for (partIndex in resume.partIndex until parts.size) {
            while (true) {
                when (waitForContinueOrCancel(downloadId, lastProgress, notificationHelper)) {
                    ControlAction.Cancel -> return DownloadWorkProgress.canceled(lastProgress)
                    ControlAction.Continue -> Unit
                }

                val part = parts[partIndex]
                val currentResume = resolveMultipartResumeState(outputFile.length(), parts)
                val sourceOffset =
                    if (partIndex == currentResume.partIndex) {
                        currentResume.partSourceOffset
                    } else {
                        0L
                    }
                val overallReceivedBytes =
                    resolveOverallReceivedBytes(
                        outputFile = outputFile,
                        parts = parts,
                        partIndex = partIndex,
                        sourceOffset = sourceOffset,
                    )

                lastProgress = syncProgressFromFile(
                    outputFile = outputFile,
                    lastProgress = lastProgress,
                    totalBytes = totalBytes,
                    totalParts = parts.size,
                    currentPartIndex = partIndex,
                    allParts = parts,
                )
                publishProgress(notificationHelper, lastProgress)

                val outcome = executeSegment(
                    downloader = downloader,
                    outputFile = outputFile,
                    url = part.url,
                    headers = part.headers,
                    expectedSegmentBytes = part.fileSize ?: 0L,
                    appendToOutput = outputFile.exists() && outputFile.length() > 0L,
                    sourceOffset = sourceOffset,
                    overallReceivedBytes = overallReceivedBytes,
                    overallTotalBytes = totalBytes,
                    allParts = parts,
                    downloadId = downloadId,
                    notificationHelper = notificationHelper,
                    totalParts = parts.size,
                    currentPartIndex = partIndex,
                    currentPartTotalBytes = part.fileSize,
                )

                lastProgress = outcome.progress ?: lastProgress
                when (outcome.type) {
                    SegmentOutcomeType.Completed -> break
                    SegmentOutcomeType.Stopped -> continue
                    SegmentOutcomeType.Failed -> {
                        val downloadError =
                            outcome.error?.let(DownloadError::fromThrowable)
                                ?: DownloadError.Unknown()
                        val failed =
                            DownloadWorkProgress.failed(
                                error = downloadError,
                                lastKnown = lastProgress,
                            )
                        publishProgress(notificationHelper, failed)
                        return failed
                    }
                }
            }

            resume = MultipartResumeState(partIndex + 1, 0L)
        }

        val completed = DownloadWorkProgress.completed(
            outputFilePath = outputFile.absolutePath,
            totalBytes = totalBytes ?: outputFile.length(),
        )
        publishProgress(notificationHelper, completed)
        return completed
    }

    /**
     * Runs one HTTP transfer segment through [OkHttpDownloader] and maps callbacks to
     * [DownloadWorkProgress] updates.
     */
    private suspend fun executeSegment(
        downloader: OkHttpDownloader,
        outputFile: File,
        url: String,
        headers: Map<String, String>,
        expectedSegmentBytes: Long,
        appendToOutput: Boolean,
        sourceOffset: Long,
        overallReceivedBytes: Long,
        overallTotalBytes: Long?,
        allParts: List<SingleItemDownloadData>,
        downloadId: String,
        notificationHelper: DownloadNotificationHelper,
        totalParts: Int,
        currentPartIndex: Int,
        currentPartTotalBytes: Long?,
    ): SegmentOutcome {
        var latestProgress: DownloadWorkProgress? = null
        var lastKnownTotalBytes: Long? = overallTotalBytes
        var outcomeType = SegmentOutcomeType.Failed
        var error: Exception? = IOException("Download did not complete")

        val listener = object : OkHttpListener {
            override suspend fun onStartDownload(file: File) = Unit

            override suspend fun onProgressUpdate(
                file: File,
                downloadedBytes: Long,
                downloadRate: Float,
                remainingTime: Float,
                totalBytes: Long?,
                segmentTotalBytes: Long?,
            ) {
                val resolvedSegmentTotal = segmentTotalBytes ?: currentPartTotalBytes
                val effectiveTotal = resolveOverallTotalBytes(
                    configuredTotal = overallTotalBytes,
                    totalParts = totalParts,
                    allParts = allParts,
                    currentPartIndex = currentPartIndex,
                    resolvedSegmentTotal = resolvedSegmentTotal,
                ) ?: totalBytes ?: lastKnownTotalBytes
                if (effectiveTotal != null) {
                    lastKnownTotalBytes = effectiveTotal
                }
                val partReceived = (downloadedBytes - overallReceivedBytes).coerceAtLeast(0L)
                val partTotal = currentPartTotalBytes ?: resolvedSegmentTotal
                val receivedBytes =
                    if (hasUnknownPartSizes(allParts)) {
                        outputFile.length()
                    } else {
                        downloadedBytes
                    }
                val progressUpdate = if (totalParts > 1) {
                    DownloadWorkProgress.downloadingMultiPart(
                        currentPartIndex = currentPartIndex,
                        totalParts = totalParts,
                        receivedBytes = receivedBytes,
                        totalBytes = effectiveTotal,
                        currentPartReceivedBytes = partReceived,
                        currentPartTotalBytes = partTotal,
                        downloadRate = downloadRate.toLong(),
                        remainingTimeMs = remainingTime.toLong(),
                    )
                } else {
                    DownloadWorkProgress.downloadingSingleFile(
                        receivedBytes = receivedBytes,
                        totalBytes = effectiveTotal,
                        downloadRate = downloadRate.toLong(),
                        remainingTimeMs = remainingTime.toLong(),
                    )
                }
                latestProgress = progressUpdate
                publishProgress(notificationHelper, progressUpdate)
            }

            override suspend fun onDownloadCompleted(file: File) {
                outcomeType = SegmentOutcomeType.Completed
                error = null
            }

            override suspend fun onDownloadFailed(file: File?, e: Exception) {
                outcomeType =
                    when {
                        DownloadWorkerControl.isPauseRequested(downloadId) ->
                            SegmentOutcomeType.Stopped

                        DownloadWorkerControl.isCancelRequested(downloadId) ->
                            SegmentOutcomeType.Stopped

                        e.message == DownloadError.STOPPED_MESSAGE ->
                            SegmentOutcomeType.Stopped

                        else -> SegmentOutcomeType.Failed
                    }
                error = e
            }
        }

        downloader.startDownload(
            listener = listener,
            fileUrl = url,
            expectedSegmentBytes = expectedSegmentBytes,
            headers = headers,
            outputFile = outputFile,
            appendToOutput = appendToOutput,
            sourceOffset = sourceOffset,
            overallReceivedBytes = overallReceivedBytes,
            overallTotalBytes = lastKnownTotalBytes ?: overallTotalBytes,
            downloadId = downloadId,
            shouldStop = { DownloadWorkerControl.shouldStop(downloadId) },
        )

        if (outcomeType == SegmentOutcomeType.Stopped) {
            val synced = syncProgressFromFile(
                outputFile = outputFile,
                lastProgress = latestProgress ?: DownloadWorkProgress.pending(
                    totalBytes = lastKnownTotalBytes ?: overallTotalBytes,
                    totalParts = totalParts,
                ),
                totalBytes = lastKnownTotalBytes ?: overallTotalBytes,
                totalParts = totalParts,
                currentPartIndex = currentPartIndex,
                allParts = allParts,
            )
            latestProgress =
                if (DownloadWorkerControl.isPauseRequested(downloadId)) {
                    DownloadWorkProgress.paused(synced)
                } else {
                    synced
                }
            publishProgress(notificationHelper, latestProgress)
        }

        return SegmentOutcome(
            type = outcomeType,
            progress = latestProgress,
            error = error,
        )
    }

    /**
     * Blocks while pause is active and returns whether the caller should continue or cancel.
     */
    private suspend fun waitForContinueOrCancel(
        downloadId: String,
        lastProgress: DownloadWorkProgress,
        notificationHelper: DownloadNotificationHelper,
    ): ControlAction {
        while (true) {
            if (DownloadWorkerControl.isCancelRequested(downloadId)) {
                return ControlAction.Cancel
            }

            if (!DownloadWorkerControl.isPauseRequested(downloadId)) {
                return ControlAction.Continue
            }

            while (DownloadWorkerControl.isPauseRequested(downloadId)) {
                publishProgress(notificationHelper, DownloadWorkProgress.paused(lastProgress))
                if (DownloadWorkerControl.isCancelRequested(downloadId)) {
                    return ControlAction.Cancel
                }
                DownloadWorkerControl.awaitControlSignal(downloadId, PAUSE_POLL_INTERVAL_MS)
            }

            if (DownloadWorkerControl.isCancelRequested(downloadId)) {
                return ControlAction.Cancel
            }
            // Pause was cleared; loop again in case pause/stop was requested before continuing.
        }
    }

    /** Publishes progress to WorkManager observers and refreshes the foreground notification. */
    private suspend fun publishProgress(
        notificationHelper: DownloadNotificationHelper,
        progress: DownloadWorkProgress,
    ) {
        setProgress(progress.toProgressData())
        setForeground(createForegroundInfo(notificationHelper, progress))
    }

    /** Builds [ForegroundInfo] with the data-sync service type on API 29+. */
    private fun createForegroundInfo(
        notificationHelper: DownloadNotificationHelper,
        progress: DownloadWorkProgress,
    ): ForegroundInfo {
        val notification = notificationHelper.buildNotification(progress)

        return ForegroundInfo(
                notificationHelper.notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
    }

    /**
     * Resume coordinates for a multipart job derived from the current output file length.
     *
     * @property partIndex Index of the part to download next.
     * @property partSourceOffset Byte offset within [partIndex] when resuming a partial part.
     */
    private data class MultipartResumeState(
        val partIndex: Int,
        val partSourceOffset: Long,
    )

    /** Result of waiting on user pause/cancel controls before starting or resuming a segment. */
    private enum class ControlAction {
        /** Proceed with download work. */
        Continue,

        /** User requested cancellation. */
        Cancel,
    }

    /** Outcome of a single HTTP transfer segment. */
    private enum class SegmentOutcomeType {
        /** Segment finished successfully. */
        Completed,

        /** Segment stopped cooperatively (pause or cancel). */
        Stopped,

        /** Segment failed with an error. */
        Failed,
    }

    /**
     * Aggregated result of [executeSegment].
     *
     * @property type Terminal classification of the segment run.
     * @property progress Last published progress snapshot, if any.
     * @property error Failure cause when [type] is [SegmentOutcomeType.Failed] or [SegmentOutcomeType.Stopped].
     */
    private data class SegmentOutcome(
        val type: SegmentOutcomeType,
        val progress: DownloadWorkProgress?,
        val error: Exception?,
    )

    /**
     * Returns the combined byte length of all parts before [partIndex].
     *
     * Parts with unknown size contribute `0` to the sum.
     */
    private fun bytesBeforePart(parts: List<SingleItemDownloadData>, partIndex: Int): Long =
        parts.take(partIndex).sumOf { it.fileSize ?: 0L }

    /**
     * Resolves cumulative received bytes before the active segment starts.
     *
     * When part sizes are unknown, derives the value from bytes already on disk.
     */
    private fun resolveOverallReceivedBytes(
        outputFile: File,
        parts: List<SingleItemDownloadData>,
        partIndex: Int,
        sourceOffset: Long,
    ): Long {
        if (parts.none { it.fileSize == null }) {
            return bytesBeforePart(parts, partIndex)
        }
        return (outputFile.length() - sourceOffset).coerceAtLeast(0L)
    }

    private fun hasUnknownPartSizes(parts: List<SingleItemDownloadData>): Boolean =
        parts.any { it.fileSize == null }

    /**
     * Rebuilds progress from the bytes already persisted on disk before resuming a segment.
     */
    private suspend fun syncProgressFromFile(
        outputFile: File,
        lastProgress: DownloadWorkProgress,
        totalBytes: Long?,
        totalParts: Int,
        currentPartIndex: Int,
        allParts: List<SingleItemDownloadData>,
    ): DownloadWorkProgress {
        val receivedBytes = outputFile.length()
        val effectiveTotal = totalBytes ?: lastProgress.totalBytes
        val resumeState = resolveMultipartResumeState(receivedBytes, allParts)
        val currentPartReceivedBytes =
            if (hasUnknownPartSizes(allParts)) {
                if (currentPartIndex == resumeState.partIndex) {
                    resumeState.partSourceOffset
                } else {
                    0L
                }
            } else {
                (receivedBytes - bytesBeforePart(allParts, currentPartIndex)).coerceAtLeast(0L)
            }
        val currentPartTotalBytes = allParts.getOrNull(currentPartIndex)?.fileSize
            ?: lastProgress.currentPartTotalBytes

        val synced = if (totalParts > 1) {
            DownloadWorkProgress.downloadingMultiPart(
                currentPartIndex = currentPartIndex,
                totalParts = totalParts,
                receivedBytes = receivedBytes,
                totalBytes = effectiveTotal,
                currentPartReceivedBytes = currentPartReceivedBytes,
                currentPartTotalBytes = currentPartTotalBytes,
                downloadRate = lastProgress.downloadRate,
                remainingTimeMs = lastProgress.remainingTimeMs,
            )
        } else {
            DownloadWorkProgress.downloadingSingleFile(
                receivedBytes = receivedBytes,
                totalBytes = effectiveTotal,
                downloadRate = lastProgress.downloadRate,
                remainingTimeMs = lastProgress.remainingTimeMs,
            )
        }

        return synced
    }

    /**
     * Resolves the overall file size for progress when it was not configured up front.
     */
    private fun resolveOverallTotalBytes(
        configuredTotal: Long?,
        totalParts: Int,
        allParts: List<SingleItemDownloadData>,
        currentPartIndex: Int,
        resolvedSegmentTotal: Long?,
    ): Long? {
        if (configuredTotal != null) return configuredTotal
        if (totalParts == 1) return resolvedSegmentTotal
        if (allParts.isEmpty() || resolvedSegmentTotal == null) return null

        var sum = 0L
        for (index in allParts.indices) {
            val partSize = when {
                index < currentPartIndex -> allParts[index].fileSize
                index == currentPartIndex -> resolvedSegmentTotal
                else -> allParts[index].fileSize
            } ?: return null
            sum += partSize
        }
        return sum
    }

    /**
     * Maps the current output file length to the part index and in-part offset to resume from.
     *
     * When a part size is unknown, the current part is assumed to contain all remaining bytes.
     */
    private fun resolveMultipartResumeState(
        fileLength: Long,
        parts: List<SingleItemDownloadData>,
    ): MultipartResumeState {
        if (fileLength == 0L) return MultipartResumeState(partIndex = 0, partSourceOffset = 0L)

        var consumed = 0L
        parts.forEachIndexed { index, part ->
            val partSize = part.fileSize
            when {
                partSize == null -> {
                    return MultipartResumeState(
                        partIndex = index,
                        partSourceOffset = (fileLength - consumed).coerceAtLeast(0L),
                    )
                }

                fileLength < consumed + partSize -> {
                    return MultipartResumeState(
                        partIndex = index,
                        partSourceOffset = fileLength - consumed,
                    )
                }

                fileLength == consumed + partSize -> {
                    return MultipartResumeState(
                        partIndex = index + 1,
                        partSourceOffset = 0L,
                    )
                }

                else -> consumed += partSize
            }
        }

        return MultipartResumeState(partIndex = parts.size, partSourceOffset = 0L)
    }

    companion object {
        /** Poll interval while waiting for resume after pause. */
        private const val PAUSE_POLL_INTERVAL_MS = 100L
    }
}
