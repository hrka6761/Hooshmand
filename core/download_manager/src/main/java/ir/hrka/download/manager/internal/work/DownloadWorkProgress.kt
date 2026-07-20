package ir.hrka.download.manager.internal.work

import androidx.work.Data
import androidx.work.WorkInfo
import ir.hrka.download.manager.error.DownloadError
import ir.hrka.download.manager.model.DownloadStatus
import ir.hrka.download.manager.model.DownloadType

/**
 * Runtime progress snapshot published by the download worker through `WorkManager.setProgress()`.
 *
 * Observers (notification builder, [ir.hrka.download.manager.api.DownloadListener] bridge) read
 * this object from [WorkInfo.getProgress] to render status, progress bar, speed, and ETA.
 *
 * For [DownloadType.MultiParts], [currentPartIndex] and [totalParts] describe which segment is
 * active, while [receivedBytes] and [totalBytes] always reflect **overall** progress across all
 * appended parts. [currentPartReceivedBytes] and [currentPartTotalBytes] expose per-part detail
 * for accurate multipart notification text (for example, "Part 2 of 5 — 45%").
 *
 * @property status Current lifecycle state.
 * @property receivedBytes Total bytes written to the output file so far.
 * @property totalBytes Expected final file size, if known.
 * @property downloadRate Current transfer speed in bytes per second.
 * @property remainingTimeMs Estimated time until completion in milliseconds.
 * @property progress Completion ratio in `0.0`–`1.0`, or `-1` when [totalBytes] is unknown.
 * @property currentPartIndex Zero-based index of the part currently downloading; `0` for single-file jobs.
 * @property totalParts Total number of parts; `1` for single-file jobs.
 * @property currentPartReceivedBytes Bytes received for the active part only.
 * @property currentPartTotalBytes Expected size of the active part, if known.
 * @property errorMessage Failure reason when [status] is [DownloadStatus.Failed].
 * @property errorCode Stable [DownloadError.code] when [status] is [DownloadStatus.Failed].
 * @property outputFilePath Absolute path to the saved file when [status] is [DownloadStatus.Completed].
 */
internal data class DownloadWorkProgress(
    val status: DownloadStatus,
    val receivedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val downloadRate: Long = 0L,
    val remainingTimeMs: Long = 0L,
    val progress: Float = -1f,
    val currentPartIndex: Int = 0,
    val totalParts: Int = 1,
    val currentPartReceivedBytes: Long = 0L,
    val currentPartTotalBytes: Long? = null,
    val errorMessage: String? = null,
    val errorCode: String? = null,
    val outputFilePath: String? = null,
) {

    /** Serializes this snapshot into WorkManager progress [Data]. */
    fun toProgressData(): Data = Data.Builder()
        .putString(KEY_STATUS, status.name)
        .putLong(KEY_RECEIVED_BYTES, receivedBytes)
        .putLong(KEY_TOTAL_BYTES, totalBytes ?: DownloadWorkInput.INVALID_SIZE)
        .putLong(KEY_DOWNLOAD_RATE, downloadRate)
        .putLong(KEY_REMAINING_TIME_MS, remainingTimeMs)
        .putFloat(KEY_PROGRESS, progress)
        .putInt(KEY_CURRENT_PART_INDEX, currentPartIndex)
        .putInt(KEY_TOTAL_PARTS, totalParts)
        .putLong(KEY_CURRENT_PART_RECEIVED_BYTES, currentPartReceivedBytes)
        .putLong(
            KEY_CURRENT_PART_TOTAL_BYTES,
            currentPartTotalBytes ?: DownloadWorkInput.INVALID_SIZE,
        )
        .putString(KEY_ERROR_MESSAGE, errorMessage)
        .putString(KEY_ERROR_CODE, errorCode)
        .putString(KEY_OUTPUT_FILE_PATH, outputFilePath)
        .build()

    /** Returns a copy with [progress] recalculated from known byte counts. */
    fun withCalculatedProgress(): DownloadWorkProgress {
        val calculated = when {
            totalBytes != null && totalBytes > 0L ->
                (receivedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)

            currentPartTotalBytes != null && currentPartTotalBytes > 0L ->
                (currentPartReceivedBytes.toFloat() / currentPartTotalBytes).coerceIn(0f, 1f)

            else -> -1f
        }
        return copy(progress = calculated)
    }

    companion object {

        private const val KEY_STATUS = "progress_status"
        private const val KEY_RECEIVED_BYTES = "progress_received_bytes"
        private const val KEY_TOTAL_BYTES = "progress_total_bytes"
        private const val KEY_DOWNLOAD_RATE = "progress_download_rate"
        private const val KEY_REMAINING_TIME_MS = "progress_remaining_time_ms"
        private const val KEY_PROGRESS = "progress_ratio"
        private const val KEY_CURRENT_PART_INDEX = "progress_current_part_index"
        private const val KEY_TOTAL_PARTS = "progress_total_parts"
        private const val KEY_CURRENT_PART_RECEIVED_BYTES = "progress_current_part_received_bytes"
        private const val KEY_CURRENT_PART_TOTAL_BYTES = "progress_current_part_total_bytes"
        private const val KEY_ERROR_MESSAGE = "progress_error_message"
        private const val KEY_ERROR_CODE = "progress_error_code"
        private const val KEY_OUTPUT_FILE_PATH = "progress_output_file_path"

        /** Restores [DownloadWorkProgress] from WorkManager progress [Data]. */
        fun fromProgressData(data: Data): DownloadWorkProgress {
            val status = data.getString(KEY_STATUS)?.let { DownloadStatus.valueOf(it) }
                ?: DownloadStatus.Pending

            val rawTotalBytes = data.getLong(KEY_TOTAL_BYTES, DownloadWorkInput.INVALID_SIZE)
            val rawPartTotalBytes = data.getLong(
                KEY_CURRENT_PART_TOTAL_BYTES,
                DownloadWorkInput.INVALID_SIZE,
            )

            return DownloadWorkProgress(
                status = status,
                receivedBytes = data.getLong(KEY_RECEIVED_BYTES, 0L),
                totalBytes = rawTotalBytes.takeIf { it != DownloadWorkInput.INVALID_SIZE },
                downloadRate = data.getLong(KEY_DOWNLOAD_RATE, 0L),
                remainingTimeMs = data.getLong(KEY_REMAINING_TIME_MS, 0L),
                progress = data.getFloat(KEY_PROGRESS, -1f),
                currentPartIndex = data.getInt(KEY_CURRENT_PART_INDEX, 0),
                totalParts = data.getInt(KEY_TOTAL_PARTS, 1).coerceAtLeast(1),
                currentPartReceivedBytes = data.getLong(KEY_CURRENT_PART_RECEIVED_BYTES, 0L),
                currentPartTotalBytes = rawPartTotalBytes.takeIf { it != DownloadWorkInput.INVALID_SIZE },
                errorMessage = data.getString(KEY_ERROR_MESSAGE),
                errorCode = data.getString(KEY_ERROR_CODE),
                outputFilePath = data.getString(KEY_OUTPUT_FILE_PATH),
            ).withCalculatedProgress()
        }

        /** Restores [DownloadWorkProgress] from a [WorkInfo] progress bundle. */
        fun fromWorkInfo(workInfo: WorkInfo): DownloadWorkProgress =
            fromProgressData(workInfo.progress)

        /** Initial progress before the worker starts transferring bytes. */
        fun pending(totalBytes: Long? = null, totalParts: Int = 1): DownloadWorkProgress =
            DownloadWorkProgress(
                status = DownloadStatus.Pending,
                totalBytes = totalBytes,
                totalParts = totalParts.coerceAtLeast(1),
            ).withCalculatedProgress()

        /** Active download progress for a single-file job. */
        fun downloadingSingleFile(
            receivedBytes: Long,
            totalBytes: Long?,
            downloadRate: Long,
            remainingTimeMs: Long,
        ): DownloadWorkProgress = DownloadWorkProgress(
            status = DownloadStatus.Downloading,
            receivedBytes = receivedBytes,
            totalBytes = totalBytes,
            downloadRate = downloadRate,
            remainingTimeMs = remainingTimeMs,
            currentPartIndex = 0,
            totalParts = 1,
            currentPartReceivedBytes = receivedBytes,
            currentPartTotalBytes = totalBytes,
        ).withCalculatedProgress()

        /**
         * Active download progress for a multipart job.
         *
         * @param currentPartIndex Zero-based index of the part currently being downloaded.
         * @param totalParts Total number of parts in the job.
         * @param receivedBytes Combined bytes written to the output file across all completed and active parts.
         * @param totalBytes Combined expected size of the final file, if known.
         * @param currentPartReceivedBytes Bytes received for [currentPartIndex] only.
         * @param currentPartTotalBytes Expected size of the active part, if known.
         */
        fun downloadingMultiPart(
            currentPartIndex: Int,
            totalParts: Int,
            receivedBytes: Long,
            totalBytes: Long?,
            currentPartReceivedBytes: Long,
            currentPartTotalBytes: Long?,
            downloadRate: Long,
            remainingTimeMs: Long,
        ): DownloadWorkProgress = DownloadWorkProgress(
            status = DownloadStatus.Downloading,
            receivedBytes = receivedBytes,
            totalBytes = totalBytes,
            downloadRate = downloadRate,
            remainingTimeMs = remainingTimeMs,
            currentPartIndex = currentPartIndex,
            totalParts = totalParts.coerceAtLeast(1),
            currentPartReceivedBytes = currentPartReceivedBytes,
            currentPartTotalBytes = currentPartTotalBytes,
        ).withCalculatedProgress()

        /** Paused state preserving the last known progress values. */
        fun paused(lastKnown: DownloadWorkProgress): DownloadWorkProgress =
            lastKnown.copy(status = DownloadStatus.Paused).withCalculatedProgress()

        /** Successful completion. */
        fun completed(
            outputFilePath: String,
            totalBytes: Long?,
        ): DownloadWorkProgress = DownloadWorkProgress(
            status = DownloadStatus.Completed,
            receivedBytes = totalBytes ?: 0L,
            totalBytes = totalBytes,
            progress = 1f,
            outputFilePath = outputFilePath,
        )

        /** Terminal failure. */
        fun failed(
            error: DownloadError,
            lastKnown: DownloadWorkProgress? = null,
        ): DownloadWorkProgress = (lastKnown ?: pending()).copy(
            status = DownloadStatus.Failed,
            errorMessage = error.userMessage,
            errorCode = error.code,
        )

        /** User- or system-initiated cancellation. */
        fun canceled(lastKnown: DownloadWorkProgress? = null): DownloadWorkProgress =
            (lastKnown ?: pending()).copy(status = DownloadStatus.Canceled)
    }
}
