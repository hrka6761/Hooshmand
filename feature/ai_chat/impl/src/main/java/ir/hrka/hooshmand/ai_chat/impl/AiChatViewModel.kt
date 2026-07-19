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
import ir.hrka.hooshmand.data.repository.ChatHistoryRepository
import ir.hrka.hooshmand.model.Conversation
import ir.hrka.llm.runtime.api.LlmGenerationEvent
import ir.hrka.llm.runtime.api.LlmInferenceRequest
import ir.hrka.llm.runtime.api.LlmRuntime
import ir.hrka.llm.runtime.api.LlmRuntimeFactory
import ir.hrka.llm.runtime.api.LlmRuntimeState
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI chat screen.
 *
 * Owns model-download orchestration and on-device chat via [LlmRuntime]: initialize when the
 * model file is ready, stream replies into [AiChatUiState.messages], persist completed turns
 * through [ChatHistoryRepository], and apply settings from the config dialog.
 *
 * @property appContext Application context used for downloads and [LlmRuntimeFactory].
 * @property preferencesRepository Persists and resolves the downloaded model path.
 * @property modelFileLocator Locates/validates model files and download targets.
 * @property chatHistoryRepository Local conversation and message persistence.
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val preferencesRepository: AiModelPreferencesRepository,
    private val modelFileLocator: AiModelFileLocator,
    private val chatHistoryRepository: ChatHistoryRepository,
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
    private var conversationId: String? = null
    private var conversationMessagesLoaded: Boolean = false
    private var runtimeHistorySeeded: Boolean = false

    init {
        viewModelScope.launch {
            val storedSettings = preferencesRepository.getModelSettings()
            _uiState.update { it.copy(modelSettings = storedSettings) }
            refreshModelStatus()
        }
    }

    /**
     * Binds this screen to a conversation and loads any saved messages.
     *
     * Safe to call once per navigation entry. Does not overwrite in-memory messages if the
     * user already started chatting before the load finished. After load, seeds the LiteRT
     * session when the runtime is ready so the model remembers prior turns.
     *
     * @param conversationId Stable conversation id from [ir.hrka.hooshmand.ai_chat.api.AiChatNavKey].
     */
    fun bindConversation(conversationId: String) {
        if (this.conversationId == conversationId) return
        this.conversationId = conversationId
        conversationMessagesLoaded = false
        runtimeHistorySeeded = false
        viewModelScope.launch {
            val storedMessages =
                chatHistoryRepository.observeMessages(conversationId).first()
                    .map { it.toUiMessage() }
            _uiState.update { state ->
                if (state.messages.isNotEmpty()) {
                    state
                } else {
                    state.copy(messages = storedMessages)
                }
            }
            conversationMessagesLoaded = true
            seedRuntimeHistoryIfNeeded()
        }
    }

    /**
     * Re-checks whether a valid model file exists and either starts the chat runtime or
     * shows the download dialog.
     */
    fun refreshModelStatus() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingModel = true,
                    needsPublicStoragePermission = false,
                    errorMessage = null,
                )
            }
            val isReady = preferencesRepository.isModelReady()
            if (isReady) {
                val modelPath = preferencesRepository.getModelFilePath()
                if (modelFileLocator.requiresPublicStoragePermission(modelPath)) {
                    stopDownloadObservation()
                    _uiState.update {
                        it.copy(
                            isCheckingModel = false,
                            isModelReady = false,
                            showDownloadDialog = false,
                            needsPublicStoragePermission = true,
                            selectedStorageLocation = DownloadStorageLocation.Public,
                            isDownloading = false,
                            isPaused = false,
                            downloadProgress = null,
                        )
                    }
                    return@launch
                }

                stopDownloadObservation()
                _uiState.update {
                    it.copy(
                        isCheckingModel = false,
                        isModelReady = true,
                        showDownloadDialog = false,
                        needsPublicStoragePermission = false,
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
                    needsPublicStoragePermission = false,
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
     * Called after the user grants all-files access for a public-storage model.
     *
     * Re-runs [refreshModelStatus] so the runtime can initialize.
     */
    fun onPublicStoragePermissionGranted() {
        refreshModelStatus()
    }

    /**
     * Called when the user denies all-files access for an existing public-storage model.
     *
     * Falls back to the download dialog so the model can be saved to internal storage instead.
     *
     * @param message User-visible permission error text.
     */
    fun onPublicStoragePermissionDenied(message: String) {
        _uiState.update {
            it.copy(
                needsPublicStoragePermission = false,
                showDownloadDialog = true,
                selectedStorageLocation = DownloadStorageLocation.Internal,
                errorMessage = message,
            )
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

        val now = System.currentTimeMillis()
        val isFirstMessage = _uiState.value.messages.isEmpty()
        val userMessage =
            AiChatMessage(
                id = newMessageId(),
                role = AiChatMessageRole.User,
                text = prompt,
                createdAt = now,
            )
        val modelMessageId = newMessageId()
        val modelMessage =
            AiChatMessage(
                id = modelMessageId,
                role = AiChatMessageRole.Model,
                text = "",
                isStreaming = true,
                createdAt = now + 1,
            )

        _uiState.update {
            it.copy(
                inputText = "",
                isGenerating = true,
                runtimeErrorMessage = null,
                messages = it.messages + userMessage + modelMessage,
            )
        }

        viewModelScope.launch {
            persistOutgoingTurn(
                userMessage = userMessage,
                titleSeed = prompt,
                isFirstMessage = isFirstMessage,
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
     * Stops the in-flight model generation, if any, and persists any partial model reply.
     */
    fun stopGeneration() {
        llmRuntime?.stopGeneration()
        generationJob?.cancel()
        generationJob = null
        val stoppedMessages =
            _uiState.value.messages.map { message ->
                if (message.isStreaming) message.copy(isStreaming = false) else message
            }
        _uiState.update { state ->
            state.copy(
                isGenerating = false,
                messages = stoppedMessages,
            )
        }
        viewModelScope.launch {
            stoppedMessages
                .filter { it.role == AiChatMessageRole.Model && it.text.isNotBlank() }
                .forEach { persistMessage(it) }
            touchConversationUpdatedAt()
        }
    }

    /**
     * Clears on-screen messages, deletes persisted messages for this conversation, and resets
     * the runtime session.
     *
     * Keeps the conversation row and the loaded model in memory. Uses the current system
     * instruction from [AiChatUiState.modelSettings].
     */
    fun clearConversation() {
        if (!_uiState.value.isModelReady) return
        if (_uiState.value.isModelInitializing) return

        llmRuntime?.stopGeneration()
        generationJob?.cancel()
        generationJob = null
        val id = conversationId
        _uiState.update {
            it.copy(
                isGenerating = false,
                messages = emptyList(),
                inputText = "",
                runtimeErrorMessage = null,
            )
        }

        runtimeHistorySeeded = true
        viewModelScope.launch {
            if (id != null) {
                chatHistoryRepository.deleteMessagesForConversation(id)
            }
            val runtime = llmRuntime ?: return@launch
            runCatching {
                runtime.resetConversation(
                    systemInstruction = _uiState.value.modelSettings.systemInstruction,
                    initialMessages = emptyList(),
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        runtimeErrorMessage = error.message
                            ?: "Failed to reset conversation.",
                    )
                }
            }
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
     * Applies new model settings, persists them, closes the dialog, and re-initializes the runtime.
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
        viewModelScope.launch {
            preferencesRepository.saveModelSettings(settings)
            initializeRuntime()
        }
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
        // Cancel in-flight model load so leaving the screen does not keep initializing.
        initializeJob?.cancel()
        initializeJob = null
        val runtime = llmRuntime
        llmRuntime = null
        if (runtime != null) {
            // Do not runBlocking on the main thread: close() can wait on native init.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { runtime.stopGeneration() }
                runCatching { runtime.close() }
            }
        }
        super.onCleared()
    }

    /**
     * Loads the on-device model into [llmRuntime] using the current [AiChatUiState.modelSettings].
     *
     * After a successful load, seeds prior conversation turns into the LiteRT session when they
     * are already available from Room.
     */
    private fun initializeRuntime() {
        initializeJob?.cancel()
        initializeJob =
            viewModelScope.launch {
                runtimeHistorySeeded = false
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
                    // Seed before clearing the initializing flag so send stays blocked.
                    seedRuntimeHistoryIfNeeded()
                    _uiState.update {
                        it.copy(isModelInitializing = false, runtimeErrorMessage = null)
                    }
                } catch (cancelled: CancellationException) {
                    // ViewModel was cleared (or a newer init replaced this job).
                    throw cancelled
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
     * Restores saved USER/MODEL turns into the LiteRT conversation when both Room load and
     * runtime init have finished.
     *
     * Safe to call from either path; runs at most once until the next init/bind/clear.
     */
    private suspend fun seedRuntimeHistoryIfNeeded() {
        if (runtimeHistorySeeded) return
        if (!conversationMessagesLoaded) return
        val runtime = llmRuntime ?: return
        if (runtime.state != LlmRuntimeState.READY) return

        val historyMessages = _uiState.value.messages.toLlmHistoryMessages()
        if (historyMessages.isEmpty()) {
            runtimeHistorySeeded = true
            return
        }

        runCatching {
            runtime.resetConversation(
                systemInstruction = _uiState.value.modelSettings.systemInstruction,
                initialMessages = historyMessages,
            )
            runtimeHistorySeeded = true
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    runtimeErrorMessage = error.message
                        ?: "Failed to restore conversation context.",
                )
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
     * Marks the model message as finished streaming and persists it.
     *
     * @param messageId Target model message id.
     */
    private fun finishModelMessage(messageId: String) {
        var finished: AiChatMessage? = null
        _uiState.update { state ->
            state.copy(
                messages =
                    state.messages.map { message ->
                        if (message.id == messageId) {
                            message.copy(isStreaming = false).also { finished = it }
                        } else {
                            message
                        }
                    },
            )
        }
        val message = finished ?: return
        viewModelScope.launch {
            if (message.text.isNotBlank()) {
                persistMessage(message)
                touchConversationUpdatedAt()
            }
        }
    }

    /**
     * Marks the model message as failed, records a runtime error, and persists the result.
     *
     * @param messageId Target model message id.
     * @param errorMessage User-visible error text.
     */
    private fun failModelMessage(messageId: String, errorMessage: String) {
        var failed: AiChatMessage? = null
        _uiState.update { state ->
            state.copy(
                runtimeErrorMessage = errorMessage,
                messages =
                    state.messages.map { message ->
                        if (message.id == messageId) {
                            message
                                .copy(
                                    text = message.text.ifBlank { errorMessage },
                                    isStreaming = false,
                                    role =
                                        if (message.text.isBlank()) {
                                            AiChatMessageRole.Error
                                        } else {
                                            message.role
                                        },
                                )
                                .also { failed = it }
                        } else {
                            message
                        }
                    },
            )
        }
        val message = failed ?: return
        viewModelScope.launch {
            persistMessage(message)
            touchConversationUpdatedAt()
        }
    }

    /**
     * Ensures the conversation row exists, then persists the user message.
     *
     * @param userMessage User turn to store.
     * @param titleSeed Text used as the conversation title when creating a new row.
     * @param isFirstMessage `true` when this is the first turn in an empty chat.
     */
    private suspend fun persistOutgoingTurn(
        userMessage: AiChatMessage,
        titleSeed: String,
        isFirstMessage: Boolean,
    ) {
        val id = conversationId ?: return
        val now = System.currentTimeMillis()
        val existing = chatHistoryRepository.observeConversation(id).first()
        val title =
            titleSeed.take(CONVERSATION_TITLE_MAX_LENGTH).ifBlank { DEFAULT_CONVERSATION_TITLE }
        val conversation =
            when {
                existing == null ->
                    Conversation(
                        id = id,
                        title = if (isFirstMessage) title else DEFAULT_CONVERSATION_TITLE,
                        createdAt = now,
                        updatedAt = now,
                    )

                existing.title.isBlank() || existing.title == DEFAULT_CONVERSATION_TITLE ->
                    existing.copy(title = title, updatedAt = now)

                else -> existing.copy(updatedAt = now)
            }
        chatHistoryRepository.upsertConversation(conversation)
        persistMessage(userMessage)
    }

    /**
     * Bumps [Conversation.updatedAt] for the bound conversation when it exists.
     */
    private suspend fun touchConversationUpdatedAt() {
        val id = conversationId ?: return
        val existing = chatHistoryRepository.observeConversation(id).first() ?: return
        chatHistoryRepository.upsertConversation(
            existing.copy(updatedAt = System.currentTimeMillis()),
        )
    }

    /**
     * Persists a single chat message for the bound conversation.
     *
     * @param message UI message to store.
     */
    private suspend fun persistMessage(message: AiChatMessage) {
        val id = conversationId ?: return
        chatHistoryRepository.upsertMessage(message.toDomainMessage(id))
    }

    /**
     * Creates a stable unique id for a chat message.
     *
     * @return New message id string.
     */
    private fun newMessageId(): String = UUID.randomUUID().toString()

    private companion object {
        const val CONVERSATION_TITLE_MAX_LENGTH = 60
        const val DEFAULT_CONVERSATION_TITLE = "New chat"
    }

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
