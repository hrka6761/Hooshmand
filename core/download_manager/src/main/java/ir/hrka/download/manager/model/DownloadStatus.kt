package ir.hrka.download.manager.model

/**
 * Lifecycle states reported by the download engine and persisted across process restarts.
 *
 * @see ir.hrka.download.manager.api.DownloadManager
 */
enum class DownloadStatus {

    /** Waiting for requirements such as network or internet connection. */
    Pending,

    /** Downloading one or more files. */
    Downloading,

    /** Download is paused and can be resumed from the last byte offset. */
    Paused,

    /**
     * Reserved for a future merge step.
     *
     * Multipart downloads currently append parts in order into one file and never publish
     * this status.
     */
    Merging,

    /** Download completed successfully. */
    Completed,

    /** Download was canceled. */
    Canceled,

    /** Download failed. */
    Failed,
}
