package ir.hrka.download.manager

/**
 * Describes how a download job is structured.
 *
 * @see DownloadManager.Builder.setDownloadType
 * @see SingleItemDownloadData
 */
enum class DownloadType {

    /**
     * A single HTTP resource is downloaded directly to one output file.
     */
    SingleFile,

    /**
     * Multiple resources are downloaded as separate parts and merged into one output file.
     */
    MultiParts,
}
