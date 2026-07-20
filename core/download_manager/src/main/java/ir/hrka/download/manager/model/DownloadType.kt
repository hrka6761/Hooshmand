package ir.hrka.download.manager.model

/**
 * Describes how a download job is structured.
 *
 * Configure via [ir.hrka.download.manager.api.DownloadManager.Builder.setSingleItemDownloadData]
 * or [ir.hrka.download.manager.api.DownloadManager.Builder.setMultiPartsDownloadData].
 *
 * @see SingleItemDownloadData
 * @see MultipartItemDownloadData
 */
enum class DownloadType {

    /**
     * A single HTTP resource is downloaded directly to one output file.
     */
    SingleFile,

    /**
     * Multiple resources are downloaded in order and appended into one output file.
     *
     * There is no separate merge step; each part is written as a continuation of the same file.
     */
    MultiParts,
}
