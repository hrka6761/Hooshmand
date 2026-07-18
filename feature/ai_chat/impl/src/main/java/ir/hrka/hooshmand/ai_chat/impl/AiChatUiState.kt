package ir.hrka.hooshmand.ai_chat.impl

import ir.hrka.download.manager.DownloadStorageLocation

/**
 * Progress reported while the model is downloading.
 *
 * @property receivedBytes Bytes written to the output file so far.
 * @property totalBytes Expected final file size in bytes, or `0` when unknown.
 * @property progress Completion ratio in `0.0`–`1.0`, or a sentinel when unknown.
 * @property remainingTimeMs Estimated time until download completion in milliseconds.
 * @property downloadRateBytesPerSec Current transfer speed in bytes per second.
 * @property currentPartIndex Zero-based index of the part currently downloading.
 * @property totalParts Total number of multipart segments.
 */
data class ModelDownloadProgress(
    val receivedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val remainingTimeMs: Long = 0L,
    val downloadRateBytesPerSec: Long = 0L,
    val currentPartIndex: Int = 0,
    val totalParts: Int = 1,
)

/**
 * UI state for the AI chat screen: model download gate, chat session, and settings.
 *
 * Download fields drive [ir.hrka.hooshmand.ai_chat.impl.ui.ModelDownloadDialog].
 * Chat fields drive the conversation UI after [isModelReady] becomes `true`.
 *
 * @property isCheckingModel `true` while verifying that a valid model file exists.
 * @property isModelReady `true` when a complete model file is available for inference.
 * @property showDownloadDialog `true` when the download dialog should be visible.
 * @property selectedStorageLocation Storage target chosen in the download dialog.
 * @property isDownloading `true` while a model download job is active or paused.
 * @property isPaused `true` when the active download is paused.
 * @property downloadProgress Latest download progress snapshot, or `null` if idle.
 * @property errorMessage Download-related error text shown in the download dialog.
 * @property messages Ordered conversation bubbles shown in the chat list.
 * @property inputText Current text in the message input field.
 * @property modelSettings User-editable inference settings for the loaded model.
 * @property isModelInitializing `true` while [ir.hrka.llm.runtime.api.LlmRuntime] is loading.
 * @property isGenerating `true` while the model is streaming a reply.
 * @property showSettingsDialog `true` when the model settings dialog should be visible.
 * @property runtimeErrorMessage Inference or runtime error text shown in the chat UI.
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
    val messages: List<AiChatMessage> = emptyList(),
    val inputText: String = "",
    val modelSettings: AiChatModelSettings = AiChatModelSettings(),
    val isModelInitializing: Boolean = false,
    val isGenerating: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val runtimeErrorMessage: String? = null,
)
