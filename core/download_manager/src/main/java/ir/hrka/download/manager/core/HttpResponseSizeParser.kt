package ir.hrka.download.manager.core

import okhttp3.Response

/**
 * Derives total byte counts from HTTP response headers for progress reporting.
 */
internal object HttpResponseSizeParser {

    /**
     * Returns the total size of the resource being downloaded.
     *
     * Prefers the total from a `Content-Range` header on partial (`206`) responses, otherwise
     * combines [sourceOffset] with `Content-Length`.
     */
    fun resolveTotalBytes(response: Response, sourceOffset: Long): Long? {
        response.header("Content-Range")?.let { contentRange ->
            parseContentRangeTotal(contentRange)?.let { return it }
        }

        val bodyLength = response.body?.contentLength() ?: -1L
        if (bodyLength < 0L) return null

        return sourceOffset + bodyLength
    }

    private fun parseContentRangeTotal(contentRange: String): Long? {
        val total = contentRange.substringAfterLast('/', missingDelimiterValue = "")
            .trim()
            .takeIf { it.isNotEmpty() && it != "*" }
            ?: return null
        return total.toLongOrNull()?.takeIf { it > 0L }
    }
}
