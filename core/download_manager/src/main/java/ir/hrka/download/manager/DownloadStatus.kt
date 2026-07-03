package ir.hrka.download.manager

/**
 * Lifecycle states reported by the download engine and persisted across process restarts.
 *
 * @see DownloadManager
 */
enum class DownloadStatus {

    /** Waiting for requirements such as network or internet connection. */
    Pending,

    /** Downloading one or more files. */
    Downloading,

    /** Download is paused and can be resumed from the last byte offset. */
    Paused,

    /** Merging downloaded parts into the final file ([DownloadType.MultiParts] only). */
    Merging,

    /** Download completed successfully. */
    Completed,

    /** Download was canceled. */
    Canceled,

    /** Download failed. */
    Failed,
}