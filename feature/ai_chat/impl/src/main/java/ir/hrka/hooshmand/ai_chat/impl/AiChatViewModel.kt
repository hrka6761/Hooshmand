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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AiChatViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val preferencesRepository: AiModelPreferencesRepository,
    private val modelFileLocator: AiModelFileLocator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val modelDownloadId: String =
        DownloadIds.multipart(AiChatModelDownloadParts.toMultipartDownloadData())

    private val modelPartCount: Int = AiChatModelDownloadParts.parts.size

    private var activeDownloadManager: DownloadManager? = null
    private var downloadProgressObserver: DownloadProgressObserver? = null

    init {
        viewModelScope.launch {
            refreshModelStatus()
        }
    }


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
                                    totalBytes = AiChatModelDownloadParts.totalSizeInBytes ?: 0L,
                                    totalParts = modelPartCount,
                                )
                        } else {
                            null
                        },
                )
            }
        }
    }

    fun onStorageLocationSelected(location: DownloadStorageLocation) {
        if (_uiState.value.isDownloading) return
        _uiState.update { it.copy(selectedStorageLocation = location, errorMessage = null) }
    }

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
                        totalBytes = AiChatModelDownloadParts.totalSizeInBytes ?: 0L,
                        totalParts = modelPartCount,
                    ),
            )
        }

        stopDownloadObservation()

        val targetFile = modelFileLocator.modelFileFor(storageLocation)
        val fileCreationMode =
            if (targetFile != null && modelFileLocator.shouldOverwriteExistingFile(targetFile))
                FileCreationMode.Overwrite
            else
                FileCreationMode.Append

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

    fun pauseModelDownload() {
        if (!_uiState.value.isDownloading || _uiState.value.isPaused) return

        activeDownloadManager?.pauseDownload()
            ?: DownloadProgressObserver.pauseDownload(appContext, modelDownloadId)
    }

    fun resumeModelDownload() {
        if (!_uiState.value.isDownloading && !_uiState.value.isPaused) return

        activeDownloadManager?.resumeDownload()
            ?: DownloadProgressObserver.resumeDownload(appContext, modelDownloadId)
        getOrCreateDownloadObserver().startObserving()
        _uiState.update { it.copy(isPaused = false, isDownloading = true) }
    }

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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onDownloadPermissionDenied(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    override fun onCleared() {
        stopDownloadObservation()
        activeDownloadManager = null
        super.onCleared()
    }

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

    private fun stopDownloadObservation() {
        downloadProgressObserver?.stopObserving()
    }

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
                                    totalBytes = AiChatModelDownloadParts.totalSizeInBytes ?: 0L,
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
                                totalBytes = AiChatModelDownloadParts.totalSizeInBytes ?: 0L,
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
