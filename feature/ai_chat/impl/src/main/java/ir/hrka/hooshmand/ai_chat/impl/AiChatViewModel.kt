package ir.hrka.hooshmand.ai_chat.impl

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.FileCreationMode
import ir.hrka.download.manager.api.DownloadIds
import ir.hrka.download.manager.api.DownloadListener
import ir.hrka.download.manager.api.DownloadManager
import ir.hrka.download.manager.api.DownloadProgressObserver
import ir.hrka.hooshmand.ai_chat.impl.data.AiModelFileLocator
import ir.hrka.hooshmand.ai_chat.impl.data.AiModelPreferencesRepository
import ir.hrka.hooshmand.ai_chat.impl.download.toMultipartDownloadData
import ir.hrka.llm.runtime.api.LlmGenerationEvent
import ir.hrka.llm.runtime.api.LlmInferenceRequest
import ir.hrka.llm.runtime.api.LlmRuntime
import ir.hrka.llm.runtime.api.LlmRuntimeFactory
import ir.hrka.llm.runtime.api.LlmRuntimeState
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel for the AI chat screen.
 *
 * Owns model-download orchestration and on-device chat via [LlmRuntime]: initialize when the
 * model file is ready, stream replies into [AiChatUiState.messages], and apply settings from
 * the config dialog.
 *
 * @property appContext Application context used for downloads and [LlmRuntimeFactory].
 * @property preferencesRepository Persists and resolves the downloaded model path.
 * @property modelFileLocator Locates/validates model files and download targets.
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val preferencesRepository: AiModelPreferencesRepository,
    private val modelFileLocator: AiModelFileLocator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())

    /** Observable UI state for download gate and chat session. */
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val modelDownloadId: String =
        DownloadIds.multipart(AiChatModelDownloadParts.toMultipartDownloadData())

    private val modelPartCount: Int = AiChatModelDownloadParts.parts.size

    private var activeDownloadManager: DownloadManager? = null
    private var downloadProgressObserver: DownloadProgressObserver? = null
    private var llmRuntime: LlmRuntime? = null
    private var generationJob: Job? = null
    private var initializeJob: Job? = null

    init {
        viewModelScope.launch {
            refreshModelStatus()
        }
    }

    /**
     * Re-checks whether a valid model file exists and either starts the chat runtime or
     * shows the download dialog.
     */
    fun refreshModelStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingModel = true, errorMessage = null) }
            val isReady = preferencesRepository.isModelReady()
            if (isReady) {
                stopDownloadObservation()
                _uiState.update {
                    it.copy(
                        isCheckingModel = false,
                        isModelReady = true,
                        showDownloadDialog = false,
                        isDownloading = false,
                        isPaused = false,
                        downloadProgress = null,
                    )
                }
                initializeRuntime()
                return@launch
            }

            val observer = getOrCreateDownloadObserver()
            val isActive = observer.isDownloadActive()
            observer.startObserving()

            _uiState.update {
                it.copy(
                    isCheckingModel = false,
                    isModelReady = false,
                    showDownloadDialog = true,
                    isDownloading = isActive,
                    downloadProgress =
                        if (isActive) {
                            it.downloadProgress
                                ?: ModelDownloadProgress(
                                    totalBytes = AiChatModelDownloadParts.totalSizeInBytes,
                                    totalParts = modelPartCount,
                                )
                        } else {
                            null
                        },
                )
            }
        }
    }

    /**
     * Updates the selected download storage location when a download is not active.
     *
     * @param location Chosen [DownloadStorageLocation].
     */
    fun onStorageLocationSelected(location: DownloadStorageLocation) {
        if (_uiState.value.isDownloading) return
        _uiState.update { it.copy(selectedStorageLocation = location, errorMessage = null) }
    }

    /**
     * Starts a new model download, or resumes when the current job is paused.
     */
    fun startModelDownload() {
        if (_uiState.value.isPaused) {
            resumeModelDownload()
            return
        }
        if (_uiState.value.isDownloading) return

        val storageLocation = _uiState.value.selectedStorageLocation
        _uiState.update {
            it.copy(
                isDownloading = true,
                isPaused = false,
                errorMessage = null,
                downloadProgress =
                    ModelDownloadProgress(
                        totalBytes = AiChatModelDownloadParts.totalSizeInBytes,
                        totalParts = modelPartCount,
                    ),
            )
        }

        stopDownloadObservation()

        val targetFile = modelFileLocator.modelFileFor(storageLocation)
        val fileCreationMode =
            if (targetFile != null && modelFileLocator.shouldOverwriteExistingFile(targetFile)) {
                FileCreationMode.Overwrite
            } else {
                FileCreationMode.Append
            }

        val downloadManager =
            DownloadManager.Builder(appContext)
                .setMultiPartsDownloadData(AiChatModelDownloadParts.toMultipartDownloadData())
                .setFileLocation(storageLocation)
                .setDirectories(listOf(AiChatModelDownloadParts.MODEL_DIRECTORY))
                .setFileCreationMode(fileCreationMode)
                .setDownloadListener(createDownloadListener())
                .build()

        activeDownloadManager = downloadManager
        downloadManager.startDownload()
    }

    /**
     * Pauses the active model download if one is running.
     */
    fun pauseModelDownload() {
        if (!_uiState.value.isDownloading || _uiState.value.isPaused) return

        activeDownloadManager?.pauseDownload()
            ?: DownloadProgressObserver.pauseDownload(appContext, modelDownloadId)
    }

    /**
     * Resumes a paused model download.
     */
    fun resumeModelDownload() {
        if (!_uiState.value.isDownloading && !_uiState.value.isPaused) return

        activeDownloadManager?.resumeDownload()
            ?: DownloadProgressObserver.resumeDownload(appContext, modelDownloadId)
        getOrCreateDownloadObserver().startObserving()
        _uiState.update { it.copy(isPaused = false, isDownloading = true) }
    }

    /**
     * Cancels the active model download and clears download progress UI.
     */
    fun cancelModelDownload() {
        activeDownloadManager?.stopDownload()
            ?: DownloadProgressObserver.cancelDownload(appContext, modelDownloadId)
        activeDownloadManager = null
        stopDownloadObservation()
        _uiState.update {
            it.copy(
                isDownloading = false,
                isPaused = false,
                downloadProgress = null,
            )
        }
    }

    /**
     * Clears the download-dialog error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Records a download permission denial message for the download dialog.
     *
     * @param message User-visible permission error text.
     */
    fun onDownloadPermissionDenied(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    /**
     * Updates the chat input draft text.
     *
     * @param text New input field value.
     */
    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Sends the current input as a user message and streams a model reply.
     */
    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty()) return
        if (_uiState.value.isGenerating || _uiState.value.isModelInitializing) return

        val runtime = llmRuntime
        if (runtime == null || runtime.state != LlmRuntimeState.READY) {
            _uiState.update {
                it.copy(runtimeErrorMessage = "Model is not ready yet.")
            }
            return
        }

        val userMessage =
            AiChatMessage(
                id = newMessageId(),
                role = AiChatMessageRole.User,
                text = prompt,
            )
        val modelMessageId = newMessageId()
        val modelMessage =
            AiChatMessage(
                id = modelMessageId,
                role = AiChatMessageRole.Model,
                text = "",
                isStreaming = true,
            )

        _uiState.update {
            it.copy(
                inputText = "",
                isGenerating = true,
                runtimeErrorMessage = null,
                messages = it.messages + userMessage + modelMessage,
            )
        }

        generationJob?.cancel()
        generationJob =
            viewModelScope.launch {
                try {
                    runtime
                        .generateFlow(LlmInferenceRequest(prompt = prompt))
                        .collect { event ->
                            when (event) {
                                is LlmGenerationEvent.Token -> appendModelToken(modelMessageId, event.text)
                                is LlmGenerationEvent.Thinking -> Unit
                                LlmGenerationEvent.Done -> finishModelMessage(modelMessageId)
                                is LlmGenerationEvent.Error -> {
                                    failModelMessage(
                                        messageId = modelMessageId,
                                        errorMessage = event.exception.message
                                            ?: "Generation failed.",
                                    )
                                }
                            }
                        }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    finishModelMessage(modelMessageId)
                    throw cancelled
                } catch (error: Exception) {
                    failModelMessage(
                        messageId = modelMessageId,
                        errorMessage = error.message ?: "Generation failed.",
                    )
                } finally {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
    }

    /**
     * Stops the in-flight model generation, if any.
     */
    fun stopGeneration() {
        llmRuntime?.stopGeneration()
        generationJob?.cancel()
        generationJob = null
        _uiState.update { state ->
            state.copy(
                isGenerating = false,
                messages =
                    state.messages.map { message ->
                        if (message.isStreaming) message.copy(isStreaming = false) else message
                    },
            )
        }
    }

    /**
     * Opens the model settings dialog.
     */
    fun openSettings() {
        if (!_uiState.value.isModelReady) return
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    /**
     * Closes the model settings dialog without applying changes.
     */
    fun dismissSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    /**
     * Applies new model settings, closes the dialog, and re-initializes the runtime.
     *
     * @param settings Confirmed settings from the dialog.
     */
    fun confirmSettings(settings: AiChatModelSettings) {
        stopGeneration()
        _uiState.update {
            it.copy(
                modelSettings = settings,
                showSettingsDialog = false,
                runtimeErrorMessage = null,
            )
        }
        initializeRuntime()
    }

    /**
     * Clears a chat/runtime error banner.
     */
    fun clearRuntimeError() {
        _uiState.update { it.copy(runtimeErrorMessage = null) }
    }

    override fun onCleared() {
        stopDownloadObservation()
        activeDownloadManager = null
        generationJob?.cancel()
        generationJob = null
        initializeJob?.cancel()
        initializeJob = null
        llmRuntime?.stopGeneration()
        val runtime = llmRuntime
        llmRuntime = null
        if (runtime != null) {
            runBlocking {
                runCatching { runtime.close() }
            }
        }
        super.onCleared()
    }

    /**
     * Loads the on-device model into [llmRuntime] using the current [AiChatUiState.modelSettings].
     */
    private fun initializeRuntime() {
        initializeJob?.cancel()
        initializeJob =
            viewModelScope.launch {
                _uiState.update {
                    it.copy(isModelInitializing = true, runtimeErrorMessage = null)
                }

                val modelPath =
                    preferencesRepository.getModelFilePath().ifBlank {
                        modelFileLocator.resolveValidModelPath(null).orEmpty()
                    }
                if (modelPath.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isModelInitializing = false,
                            runtimeErrorMessage = "Model file path is unavailable.",
                        )
                    }
                    return@launch
                }

                val runtime = llmRuntime ?: LlmRuntimeFactory.create(appContext).also { llmRuntime = it }
                try {
                    runtime.initialize(
                        _uiState.value.modelSettings.toRuntimeConfig(modelPath = modelPath),
                    )
                    _uiState.update {
                        it.copy(isModelInitializing = false, runtimeErrorMessage = null)
                    }
                } catch (error: Exception) {
                    _uiState.update {
                        it.copy(
                            isModelInitializing = false,
                            runtimeErrorMessage = error.message ?: "Failed to initialize model.",
                        )
                    }
                }
            }
    }

    /**
     * Appends a streamed token to the model message identified by [messageId].
     *
     * @param messageId Target model message id.
     * @param token Text token from the runtime.
     */
    private fun appendModelToken(messageId: String, token: String) {
        _uiState.update { state ->
            state.copy(
                messages =
                    state.messages.map { message ->
                        if (message.id == messageId) {
                            message.copy(text = message.text + token, isStreaming = true)
                        } else {
                            message
                        }
                    },
            )
        }
    }

    /**
     * Marks the model message as finished streaming.
     *
     * @param messageId Target model message id.
     */
    private fun finishModelMessage(messageId: String) {
        _uiState.update { state ->
            state.copy(
                messages =
                    state.messages.map { message ->
                        if (message.id == messageId) message.copy(isStreaming = false) else message
                    },
            )
        }
    }

    /**
     * Marks the model message as failed and records a runtime error.
     *
     * @param messageId Target model message id.
     * @param errorMessage User-visible error text.
     */
    private fun failModelMessage(messageId: String, errorMessage: String) {
        _uiState.update { state ->
            state.copy(
                runtimeErrorMessage = errorMessage,
                messages =
                    state.messages.map { message ->
                        if (message.id == messageId) {
                            message.copy(
                                text = message.text.ifBlank { errorMessage },
                                isStreaming = false,
                                role =
                                    if (message.text.isBlank()) {
                                        AiChatMessageRole.Error
                                    } else {
                                        message.role
                                    },
                            )
                        } else {
                            message
                        }
                    },
            )
        }
    }

    /**
     * Creates a stable unique id for a chat message.
     *
     * @return New message id string.
     */
    private fun newMessageId(): String = UUID.randomUUID().toString()

    /**
     * Returns an existing download observer or creates one for [modelDownloadId].
     *
     * @return Observer attached to this ViewModel scope.
     */
    private fun getOrCreateDownloadObserver(): DownloadProgressObserver {
        val existing = downloadProgressObserver
        if (existing != null) return existing

        return DownloadProgressObserver(
            context = appContext,
            downloadId = modelDownloadId,
            listener = createDownloadListener(),
            scope = viewModelScope,
        ).also { downloadProgressObserver = it }
    }

    /**
     * Stops observing download WorkManager progress without cancelling the job.
     */
    private fun stopDownloadObservation() {
        downloadProgressObserver?.stopObserving()
    }

    /**
     * Builds the [DownloadListener] that updates download UI and initializes chat on success.
     *
     * @return Listener for active download observation.
     */
    private fun createDownloadListener(): DownloadListener =
        object : DownloadListener {
            override fun onStartDownload() {
                _uiState.update {
                    it.copy(
                        isDownloading = true,
                        isPaused = false,
                        showDownloadDialog = true,
                        downloadProgress =
                            it.downloadProgress?.copy(progress = 0f)
                                ?: ModelDownloadProgress(
                                    totalBytes = AiChatModelDownloadParts.totalSizeInBytes,
                                    totalParts = modelPartCount,
                                ),
                    )
                }
            }

            override fun onDownloading(
                receivedBytes: Long,
                downloadRate: Long,
                remainingTime: Long,
                progress: Float,
                currentPartIndex: Int,
                totalParts: Int,
            ) {
                _uiState.update {
                    it.copy(
                        isDownloading = true,
                        isPaused = false,
                        showDownloadDialog = true,
                        downloadProgress =
                            ModelDownloadProgress(
                                receivedBytes = receivedBytes,
                                totalBytes = AiChatModelDownloadParts.totalSizeInBytes,
                                progress = progress.coerceIn(0f, 1f),
                                remainingTimeMs = remainingTime,
                                downloadRateBytesPerSec = downloadRate,
                                currentPartIndex = currentPartIndex,
                                totalParts = totalParts.coerceAtLeast(1),
                            ),
                    )
                }
            }

            override fun onDownloadPaused(
                receivedBytes: Long,
                downloadRate: Long,
                remainingTime: Long,
                progress: Float,
                currentPartIndex: Int,
                totalParts: Int,
            ) {
                _uiState.update {
                    it.copy(
                        isDownloading = true,
                        isPaused = true,
                        showDownloadDialog = true,
                        downloadProgress =
                            ModelDownloadProgress(
                                receivedBytes = receivedBytes,
                                totalBytes = AiChatModelDownloadParts.totalSizeInBytes,
                                progress = progress.coerceIn(0f, 1f),
                                remainingTimeMs = remainingTime,
                                downloadRateBytesPerSec = downloadRate,
                                currentPartIndex = currentPartIndex,
                                totalParts = totalParts.coerceAtLeast(1),
                            ),
                    )
                }
            }

            override fun onDownloadSuccess(filePath: String?) {
                activeDownloadManager = null
                stopDownloadObservation()
                viewModelScope.launch {
                    if (filePath.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                downloadProgress = null,
                                errorMessage = "Download finished but file path is unavailable.",
                            )
                        }
                        return@launch
                    }

                    preferencesRepository.saveDownloadSuccess(filePath = filePath)
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            isPaused = false,
                            downloadProgress = null,
                            isModelReady = true,
                            showDownloadDialog = false,
                            errorMessage = null,
                        )
                    }
                    initializeRuntime()
                }
            }

            override fun onDownloadFailed(errorMsg: String?) {
                activeDownloadManager = null
                stopDownloadObservation()
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isPaused = false,
                        downloadProgress = null,
                        errorMessage = errorMsg ?: "Model download failed.",
                    )
                }
            }

            override fun onDownloadCancelled() {
                activeDownloadManager = null
                stopDownloadObservation()
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        isPaused = false,
                        downloadProgress = null,
                    )
                }
            }
        }
}
