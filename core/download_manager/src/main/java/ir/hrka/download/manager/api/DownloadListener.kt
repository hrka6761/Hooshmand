package ir.hrka.download.manager.api

/**
 * Callback interface for observing download lifecycle events and progress.
 *
 * Register an implementation via [DownloadManager.Builder.setDownloadListener].
 *
 * @see DownloadManager
 */
interface DownloadListener {

    /**
     * Called when the download worker starts and is about to transfer bytes.
     */
    fun onStartDownload()

    /**
     * Called periodically while bytes are being received.
     *
     * @param receivedBytes Total bytes downloaded so far.
     * @param downloadRate Current transfer speed in bytes per second.
     * @param remainingTime Estimated time until completion in milliseconds.
     * @param progress Completion ratio in the range `0.0`–`1.0`, or `-1` when unknown.
     * @param currentPartIndex Zero-based index of the active part for multipart downloads.
     * @param totalParts Total number of parts; `1` for single-file downloads.
     */
    fun onDownloading(
        receivedBytes: Long,
        downloadRate: Long,
        remainingTime: Long,
        progress: Float,
        currentPartIndex: Int = 0,
        totalParts: Int = 1,
    )

    /**
     * Called when the download finishes successfully.
     *
     * @param filePath Absolute path to the saved file, or `null` if unavailable.
     */
    fun onDownloadSuccess(filePath: String?)

    /**
     * Called when the download fails before completion.
     *
     * @param errorMsg Human-readable failure reason, or `null` if not provided.
     */
    fun onDownloadFailed(errorMsg: String?)

    /**
     * Called when the download is paused by the user or [DownloadManager.pauseDownload].
     */
    fun onDownloadPaused(
        receivedBytes: Long,
        downloadRate: Long,
        remainingTime: Long,
        progress: Float,
        currentPartIndex: Int = 0,
        totalParts: Int = 1,
    )

    /**
     * Called when the download is canceled by the user or [DownloadManager.stopDownload].
     */
    fun onDownloadCancelled()
}
