package ir.hrka.hooshmand.ai_chat.impl

import ir.hrka.download.manager.DownloadStorageLocation

/**
 * Progress reported while the model is downloading.
 */
data class ModelDownloadProgress(
    val receivedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val remainingTimeMs: Long = 0L,
    val downloadRateBytesPerSec: Long = 0L,
    /** Zero-based index of the part currently downloading. */
    val currentPartIndex: Int = 0,
    val totalParts: Int = 1,
)

/**
 * UI state for the AI chat screen and model download dialog.
 */
data class AiChatUiState(
    val isCheckingModel: Boolean = true,
    val isModelReady: Boolean = false,
    val showDownloadDialog: Boolean = false,
    val selectedStorageLocation: DownloadStorageLocation = DownloadStorageLocation.Public,
    val isDownloading: Boolean = false,
    val isPaused: Boolean = false,
    val downloadProgress: ModelDownloadProgress? = null,
    val errorMessage: String? = null,
)
