package ir.hrka.download.manager.error

import java.io.IOException

/**
 * Base exception type for download-manager failures.
 *
 * Prefer converting to [DownloadError] via [toDownloadError] before showing UI messages.
 */
sealed class DownloadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {

    /** Converts this exception into a typed [DownloadError]. */
    abstract fun toDownloadError(): DownloadError
}

/**
 * Thrown when the HTTP response is unsuccessful or has no usable body.
 */
class HttpDownloadException(
    val httpCode: Int? = null,
    message: String = "Server responded with an error or empty response.",
    cause: Throwable? = null,
) : DownloadException(message, cause) {

    override fun toDownloadError(): DownloadError =
        DownloadError.Http(httpCode = httpCode, technicalMessage = message)
}

/**
 * Thrown when a Range resume was requested but the server returned a full body (`200`)
 * instead of a partial response (`206`).
 */
class ResumeNotSupportedException(
    message: String = "Server does not support resuming this download.",
    cause: Throwable? = null,
) : DownloadException(message, cause) {

    override fun toDownloadError(): DownloadError =
        DownloadError.ResumeNotSupported(technicalMessage = message)
}

/**
 * Thrown when the HTTP response body is missing after a successful status code.
 */
class EmptyResponseException(
    message: String = "Server returned an empty response body.",
    cause: Throwable? = null,
) : DownloadException(message, cause) {

    override fun toDownloadError(): DownloadError =
        DownloadError.EmptyResponse(technicalMessage = message)
}

/**
 * Thrown when local storage cannot be used for the output file.
 */
class StorageDownloadException(
    message: String,
    cause: Throwable? = null,
) : DownloadException(message, cause) {

    override fun toDownloadError(): DownloadError =
        DownloadError.Storage(technicalMessage = message)
}

/**
 * Thrown when a required storage permission is missing.
 */
class PermissionDownloadException(
    message: String,
    cause: Throwable? = null,
) : DownloadException(message, cause) {

    override fun toDownloadError(): DownloadError =
        DownloadError.Permission(technicalMessage = message)
}
