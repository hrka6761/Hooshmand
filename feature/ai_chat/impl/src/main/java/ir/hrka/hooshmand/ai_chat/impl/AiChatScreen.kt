package ir.hrka.hooshmand.ai_chat.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.hooshmand.ai_chat.impl.ui.ModelDownloadDialog

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
        onClearError = viewModel::clearError,
        onCancel = {
            if (uiState.isDownloading)
                viewModel.cancelModelDownload()
            else
                onNavigateHome()
        },
    )
}

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
    onClearError: () -> Unit = {},
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
                    Text(
                        text = stringResource(R.string.ai_chat_ready_message),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
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
}


@Preview(showBackground = true, name = "Checking model")
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

@Preview(showBackground = true, name = "Model ready")
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

@Preview(showBackground = true, name = "Download dialog")
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

@Preview(showBackground = true, name = "Download dialog – external storage")
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

@Preview(showBackground = true, name = "Download dialog – downloading")
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

@Preview(showBackground = true, name = "Download dialog – error")
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

@Preview(showBackground = true, name = "Fallback loading")
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
