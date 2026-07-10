package ir.hrka.download.manager.api

import ir.hrka.download.manager.MultipartItemDownloadData
import ir.hrka.download.manager.SingleItemDownloadData

/**
 * Stable WorkManager unique-work identifiers for download jobs.
 *
 * These ids are used to enqueue, observe, and cancel downloads across process lifetimes.
 */
object DownloadIds {
    /** Builds the unique-work name for a multipart download. */
    fun multipart(data: MultipartItemDownloadData): String {
        val parts = data.itemParts
        val urlsHash = parts.joinToString(separator = "|") { it.url }.hashCode()
        return "multipart_${parts.first().fileName}_$urlsHash"
    }

    /** Builds the unique-work name for a single-file download. */
    fun singleFile(data: SingleItemDownloadData): String =
        "single_${data.fileName}_${data.url.hashCode()}"
}
