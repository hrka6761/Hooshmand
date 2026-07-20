package ir.hrka.download.manager.model

/**
 * Configuration for a [DownloadType.MultiParts] download job.
 *
 * Parts in [itemParts] are downloaded in list order and appended sequentially into
 * a single output file. There is no separate merge step: bytes from each part are
 * written directly to the continuation of the same file on disk.
 *
 * The final output file name is taken from [SingleItemDownloadData.fileName] on the
 * first part. Per-part [SingleItemDownloadData.fileSize] values are used for accurate
 * multipart progress; the overall total size is the sum of known part sizes.
 *
 * ## Example
 * ```
 * MultipartItemDownloadData(
 *     itemParts = listOf(
 *         SingleItemDownloadData(url = part1Url, fileName = "video.mp4", fileSize = 5_000_000L),
 *         SingleItemDownloadData(url = part2Url, fileName = "video.mp4", fileSize = 7_000_000L),
 *     ),
 * )
 * ```
 *
 * @property itemParts Non-empty ordered list of parts to download and append.
 * Each entry supplies the part URL and, when known, that part's size in bytes.
 * @property totalFileSize Combined expected size of the final file when individual part sizes
 * are unknown but the overall total is known.
 *
 * @see SingleItemDownloadData
 * @see DownloadType.MultiParts
 * @see ir.hrka.download.manager.api.DownloadManager.Builder.setMultiPartsDownloadData
 */
data class MultipartItemDownloadData(

    /**
     * Ordered parts downloaded one after another and appended into the final output file.
     */
    val itemParts: List<SingleItemDownloadData>,

    /**
     * Combined expected size of the final output file when per-part sizes are not all known.
     */
    val totalFileSize: Long? = null,
)
