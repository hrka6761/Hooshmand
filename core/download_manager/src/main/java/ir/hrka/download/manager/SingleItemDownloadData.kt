package ir.hrka.download.manager

/**
 * Describes a file to be downloaded.
 *
 * Used as the primary download target for [DownloadType.SingleFile] or as an
 * individual part for [DownloadType.MultiParts].
 *
 * @property url Direct URL of the file to download.
 * @property fileName Name to use when saving the downloaded file.
 * @property fileSize Expected size of the file in bytes, if known. Used for
 * progress reporting and optional validation.
 * @property headers Optional HTTP request headers to include when downloading
 * this file (for example, `Authorization` or `User-Agent`).
 * @property checksum Optional checksum used to verify the integrity of the
 * downloaded file after completion.
 *
 * @see DownloadManager
 * @see DownloadType
 */
data class SingleItemDownloadData(

    /**
     * Direct URL of the file to download.
     */
    val url: String,

    /**
     * Name to use when saving the downloaded file.
     */
    val fileName: String,

    /**
     * Expected size of the file in bytes, if known.
     */
    val fileSize: Long? = null,

    /**
     * Optional HTTP request headers to include with the download request.
     */
    val headers: Map<String, String> = emptyMap(),

    /**
     * Optional checksum used to verify the downloaded file.
     */
    val checksum: String? = null,
)
