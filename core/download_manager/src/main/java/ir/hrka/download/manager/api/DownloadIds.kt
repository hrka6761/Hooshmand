package ir.hrka.download.manager.api

import ir.hrka.download.manager.model.MultipartItemDownloadData
import ir.hrka.download.manager.model.SingleItemDownloadData
import java.security.MessageDigest

/**
 * Stable WorkManager unique-work identifiers for download jobs.
 *
 * These ids are used to enqueue, observe, and cancel downloads across process lifetimes.
 */
object DownloadIds {

    /** Builds the unique-work name for a multipart download. */
    fun multipart(data: MultipartItemDownloadData): String {
        val parts = data.itemParts
        val fingerprint =
            stableHash(
                parts.joinToString(separator = "|") { "${it.url}|${it.fileSize ?: ""}" },
            )
        return "multipart_${sanitize(parts.first().fileName)}_$fingerprint"
    }

    /** Builds the unique-work name for a single-file download. */
    fun singleFile(data: SingleItemDownloadData): String {
        val fingerprint = stableHash("${data.url}|${data.fileSize ?: ""}")
        return "single_${sanitize(data.fileName)}_$fingerprint"
    }

    private fun sanitize(fileName: String): String =
        fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(48)

    private fun stableHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }.take(16)
    }
}
