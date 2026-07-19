package ir.hrka.hooshmand.ai_chat.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.hooshmand.ai_chat.impl.ui.AiChatPanel
import ir.hrka.hooshmand.ai_chat.impl.ui.AiChatSettingsDialog
import ir.hrka.hooshmand.ai_chat.impl.ui.ModelDownloadDialog
import ir.hrka.hooshmand.ai_chat.impl.ui.PublicStoragePermissionDialog

/**
 * AI chat feature entry screen.
 *
 * Shows the model download gate until a valid model file is ready, then the chat panel.
 * @param modifier Optional [Modifier] for the root layout.
 * @param onNavigateHome Called when the user leaves before a download starts.
 * @param viewModel Screen ViewModel that owns download and chat/runtime state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    AiChatScreenContent(
        uiState = uiState,
        modifier = modifier,
        onStorageLocationSelected = viewModel::onStorageLocationSelected,
        onStartDownload = viewModel::startModelDownload,
        onPauseDownload = viewModel::pauseModelDownload,
        onResumeDownload = viewModel::resumeModelDownload,
        onPermissionDenied = viewModel::onDownloadPermissionDenied,
        onPublicStoragePermissionGranted = viewModel::onPublicStoragePermissionGranted,
        onPublicStoragePermissionDenied = viewModel::onPublicStoragePermissionDenied,
        onClearError = viewModel::clearError,
        onCancel = {
            if (uiState.isDownloading) {
                viewModel.cancelModelDownload()
            } else {
                onNavigateHome()
            }
        },
        onInputTextChanged = viewModel::onInputTextChanged,
        onSendMessage = viewModel::sendMessage,
        onStopGeneration = viewModel::stopGeneration,
        onOpenSettings = viewModel::openSettings,
        onDismissSettings = viewModel::dismissSettings,
        onConfirmSettings = viewModel::confirmSettings,
        onClearConversation = viewModel::clearConversation,
    )
}

/**
 * Stateless AI chat screen content used by [AiChatScreen] and Compose previews.
 *
 * @param uiState Full screen state (download gate + chat).
 * @param onStorageLocationSelected Called when the download storage option changes.
 * @param onStartDownload Called to begin or resume a model download from the dialog.
 * @param onPauseDownload Called to pause an active download.
 * @param onResumeDownload Called to resume a paused download.
 * @param onCancel Called to cancel download or leave the screen.
 * @param onPermissionDenied Called when a required download permission is denied.
 * @param onPublicStoragePermissionGranted Called after all-files access is granted for an
 * existing public-storage model.
 * @param onPublicStoragePermissionDenied Called when all-files access is denied for an
 * existing public-storage model.
 * @param onClearError Called to clear a download error message.
 * @param onInputTextChanged Called when the chat input text changes.
 * @param onSendMessage Called when the user taps Send.
 * @param onStopGeneration Called when the user taps Stop during generation.
 * @param onOpenSettings Called when the settings action in the top bar is tapped.
 * @param onDismissSettings Called when the settings dialog is dismissed.
 * @param onConfirmSettings Called when the user confirms new [AiChatModelSettings].
 * @param onClearConversation Called when the user clears the chat history.
 * @param modifier Optional [Modifier] for the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiChatScreenContent(
    uiState: AiChatUiState,
    onStorageLocationSelected: (DownloadStorageLocation) -> Unit,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancel: () -> Unit,
    onPermissionDenied: (String) -> Unit = {},
    onPublicStoragePermissionGranted: () -> Unit = {},
    onPublicStoragePermissionDenied: (String) -> Unit = {},
    onClearError: () -> Unit = {},
    onInputTextChanged: (String) -> Unit = {},
    onSendMessage: () -> Unit = {},
    onStopGeneration: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onDismissSettings: () -> Unit = {},
    onConfirmSettings: (AiChatModelSettings) -> Unit = {},
    onClearConversation: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.ai_chat_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (uiState.isModelReady) {
                        IconButton(
                            onClick = onClearConversation,
                            enabled = uiState.messages.isNotEmpty() &&
                                !uiState.isModelInitializing,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription =
                                    stringResource(R.string.ai_chat_clear_conversation_cd),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.ai_chat_settings_cd),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isCheckingModel -> {
                    CircularProgressIndicator()
                }

                uiState.isModelReady -> {
                    AiChatPanel(
                        messages = uiState.messages,
                        inputText = uiState.inputText,
                        isGenerating = uiState.isGenerating,
                        isModelInitializing = uiState.isModelInitializing,
                        runtimeErrorMessage = uiState.runtimeErrorMessage,
                        onInputTextChanged = onInputTextChanged,
                        onSendMessage = onSendMessage,
                        onStopGeneration = onStopGeneration,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.needsPublicStoragePermission -> {
                    PublicStoragePermissionDialog(
                        onPermissionGranted = onPublicStoragePermissionGranted,
                        onPermissionDenied = onPublicStoragePermissionDenied,
                        onCancel = onCancel,
                    )
                }

                uiState.showDownloadDialog -> {
                    ModelDownloadDialog(
                        uiState = uiState,
                        onStorageLocationSelected = onStorageLocationSelected,
                        onStartDownload = onStartDownload,
                        onPauseDownload = onPauseDownload,
                        onResumeDownload = onResumeDownload,
                        onCancel = onCancel,
                        onPermissionDenied = onPermissionDenied,
                        onClearError = onClearError,
                    )
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (uiState.isModelReady && uiState.showSettingsDialog) {
        AiChatSettingsDialog(
            settings = uiState.modelSettings,
            onDismissed = onDismissSettings,
            onConfirm = onConfirmSettings,
        )
    }
}

/**
 * Preview of the model-status check spinner.
 */
@Preview(showBackground = true, name = "Checking model", apiLevel = 34)
@Composable
private fun AiChatScreenCheckingModelPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState = AiChatUiState(isCheckingModel = true),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the ready chat panel with empty conversation.
 */
@Preview(showBackground = true, name = "Model ready – empty chat", apiLevel = 34)
@Composable
private fun AiChatScreenModelReadyPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    isModelReady = true,
                    showDownloadDialog = false,
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the ready chat panel with sample messages.
 */
@Preview(showBackground = true, name = "Model ready – with messages", apiLevel = 34)
@Composable
private fun AiChatScreenWithMessagesPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    isModelReady = true,
                    showDownloadDialog = false,
                    messages =
                        listOf(
                            AiChatMessage(
                                id = "1",
                                role = AiChatMessageRole.User,
                                text = "Hello",
                            ),
                            AiChatMessage(
                                id = "2",
                                role = AiChatMessageRole.Model,
                                text = "Hi! How can I help?",
                            ),
                        ),
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the download dialog with internal storage selected.
 */
@Preview(showBackground = true, name = "Download dialog", apiLevel = 34)
@Composable
private fun AiChatScreenDownloadDialogPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    showDownloadDialog = true,
                    selectedStorageLocation = DownloadStorageLocation.Internal,
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the download dialog with external storage selected.
 */
@Preview(showBackground = true, name = "Download dialog – external storage", apiLevel = 34)
@Composable
private fun AiChatScreenDownloadDialogExternalStoragePreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    showDownloadDialog = true,
                    selectedStorageLocation = DownloadStorageLocation.External,
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the download dialog while a multipart download is in progress.
 */
@Preview(showBackground = true, name = "Download dialog – downloading", apiLevel = 34)
@Composable
private fun AiChatScreenDownloadDialogDownloadingPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    showDownloadDialog = true,
                    isDownloading = true,
                    downloadProgress =
                        ModelDownloadProgress(
                            receivedBytes = 512L * 1024 * 1024,
                            totalBytes = 1024L * 1024 * 1024,
                            progress = 0.5f,
                            remainingTimeMs = 120_000L,
                            downloadRateBytesPerSec = 4_000_000L,
                            currentPartIndex = 2,
                            totalParts = 5,
                        ),
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the download dialog showing an error message.
 */
@Preview(showBackground = true, name = "Download dialog – error", apiLevel = 34)
@Composable
private fun AiChatScreenDownloadDialogErrorPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    showDownloadDialog = true,
                    errorMessage = "Download failed. Check your connection and try again.",
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}

/**
 * Preview of the fallback loading state when neither ready nor download dialog.
 */
@Preview(showBackground = true, name = "Fallback loading", apiLevel = 34)
@Composable
private fun AiChatScreenFallbackLoadingPreview() {
    MaterialTheme {
        AiChatScreenContent(
            uiState =
                AiChatUiState(
                    isCheckingModel = false,
                    isModelReady = false,
                    showDownloadDialog = false,
                ),
            onStorageLocationSelected = {},
            onStartDownload = {},
            onPauseDownload = {},
            onResumeDownload = {},
            onCancel = {},
        )
    }
}
