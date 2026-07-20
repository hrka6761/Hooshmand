package ir.hrka.download.manager.error

/**
 * Typed download failure with a stable [code] and a user-facing [userMessage].
 *
 * Consumers should show [userMessage] in the UI and may branch on [code] for recovery logic.
 */
sealed class DownloadError {

    /** Stable machine-readable identifier for this error. */
    abstract val code: String

    /** Human-readable message suitable for display to the end user. */
    abstract val userMessage: String

    /** Optional technical detail for logs (never required for UI). */
    open val technicalMessage: String? get() = null

    /** HTTP request failed or the server returned a non-success status. */
    data class Http(
        val httpCode: Int? = null,
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_HTTP
        override val userMessage: String =
            if (httpCode != null) {
                "Download failed (server error $httpCode). Please try again."
            } else {
                "Download failed because the server did not respond correctly. Please try again."
            }
    }

    /** Server ignored or rejected the HTTP Range resume request. */
    data class ResumeNotSupported(
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_RESUME_NOT_SUPPORTED
        override val userMessage: String =
            "Could not resume the download from where it left off. Please restart the download."
    }

    /** Network connectivity or I/O failure while transferring bytes. */
    data class Network(
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_NETWORK
        override val userMessage: String =
            "Download failed due to a network error. Check your connection and try again."
    }

    /** Local storage is unavailable, full, or not writable. */
    data class Storage(
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_STORAGE
        override val userMessage: String =
            "Download failed because storage is unavailable. Free up space or choose another location."
    }

    /** Required storage permission was not granted by the host app / user. */
    data class Permission(
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_PERMISSION
        override val userMessage: String =
            "Storage permission is required to save this download. Please grant access and try again."
    }

    /** Download configuration was invalid (blank URL, missing parts, etc.). */
    data class InvalidConfig(
        override val userMessage: String,
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_INVALID_CONFIG
    }

    /** Response body was empty or missing after a successful HTTP status. */
    data class EmptyResponse(
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_EMPTY_RESPONSE
        override val userMessage: String =
            "Download failed because the server returned an empty file. Please try again."
    }

    /** Unexpected failure that does not map to a more specific type. */
    data class Unknown(
        override val userMessage: String = "Download failed. Please try again.",
        override val technicalMessage: String? = null,
    ) : DownloadError() {
        override val code: String = CODE_UNKNOWN
    }

    companion object {
        const val CODE_HTTP: String = "http"
        const val CODE_RESUME_NOT_SUPPORTED: String = "resume_not_supported"
        const val CODE_NETWORK: String = "network"
        const val CODE_STORAGE: String = "storage"
        const val CODE_PERMISSION: String = "permission"
        const val CODE_INVALID_CONFIG: String = "invalid_config"
        const val CODE_EMPTY_RESPONSE: String = "empty_response"
        const val CODE_UNKNOWN: String = "unknown"
        const val CODE_STOPPED: String = "stopped"

        /**
         * Maps an arbitrary [Throwable] to a [DownloadError] for user-facing reporting.
         */
        fun fromThrowable(error: Throwable): DownloadError =
            when (error) {
                is DownloadException -> error.toDownloadError()
                is SecurityException ->
                    Permission(technicalMessage = error.message)

                is java.io.IOException ->
                    when {
                        error.message == STOPPED_MESSAGE ->
                            Unknown(
                                userMessage = "Download was stopped.",
                                technicalMessage = error.message,
                            )

                        else ->
                            Network(technicalMessage = error.message)
                    }

                is IllegalStateException ->
                    Storage(technicalMessage = error.message)

                is IllegalArgumentException ->
                    InvalidConfig(
                        userMessage = error.message
                            ?: "Download configuration is invalid.",
                        technicalMessage = error.message,
                    )

                else ->
                    Unknown(
                        userMessage = error.message ?: "Download failed. Please try again.",
                        technicalMessage = error.message,
                    )
            }

        /** Marker message used when a transfer is stopped cooperatively (pause/cancel). */
        const val STOPPED_MESSAGE: String = "Download stopped"

        /**
         * Reconstructs a [DownloadError] from WorkManager-serialized fields.
         */
        fun fromSerialized(code: String?, message: String?): DownloadError =
            when (code) {
                CODE_HTTP -> Http(technicalMessage = message)
                CODE_RESUME_NOT_SUPPORTED -> ResumeNotSupported(technicalMessage = message)
                CODE_NETWORK -> Network(technicalMessage = message)
                CODE_STORAGE -> Storage(technicalMessage = message)
                CODE_PERMISSION -> Permission(technicalMessage = message)
                CODE_INVALID_CONFIG ->
                    InvalidConfig(
                        userMessage = message ?: "Download configuration is invalid.",
                        technicalMessage = message,
                    )
                CODE_EMPTY_RESPONSE -> EmptyResponse(technicalMessage = message)
                else ->
                    Unknown(
                        userMessage = message ?: "Download failed. Please try again.",
                        technicalMessage = message,
                    )
            }
    }
}
