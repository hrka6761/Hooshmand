package ir.hrka.download.manager.core

import java.io.File

/**
 * Internal listener for low-level download events emitted by [OkHttpDownloader].
 *
 * Callbacks are `suspend` functions and are invoked from coroutine contexts used by the
 * download worker. Implementations bridge these events to the public [ir.hrka.download.manager.api.DownloadListener].
 *
 * @see OkHttpDownloader
 */
internal interface OkHttpListener {

    /**
     * Called when the download is started.
     *
     * @param file The [File] where the download will be saved.
     */
    suspend fun onStartDownload(file: File)

    /**
     * Called periodically to report progress during the download.
     *
     * @param file The [File] being downloaded.
     * @param downloadedBytes The number of bytes downloaded so far.
     * @param downloadRate The current download speed in bytes per second.
     * @param remainingTime Estimated remaining time to complete the download in milliseconds.
     * @param totalBytes Resolved overall file size for progress, when known from configuration or HTTP headers.
     * @param segmentTotalBytes Resolved size of the active URL segment, when known from HTTP headers.
     */
    suspend fun onProgressUpdate(
        file: File,
        downloadedBytes: Long,
        downloadRate: Float,
        remainingTime: Float,
        totalBytes: Long? = null,
        segmentTotalBytes: Long? = null,
    )

    /**
     * Called when the download completes successfully.
     *
     * @param file The fully downloaded [File].
     */
    suspend fun onDownloadCompleted(file: File)

    /**
     * Called when the download fails due to an exception.
     *
     * @param file The partial output [File], or `null` if resolution failed before a file was created.
     * @param e The [Exception] that caused the failure.
     */
    suspend fun onDownloadFailed(file: File?, e: Exception)
}
