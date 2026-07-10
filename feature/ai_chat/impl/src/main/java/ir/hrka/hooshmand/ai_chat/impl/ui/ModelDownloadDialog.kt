package ir.hrka.hooshmand.ai_chat.impl.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.api.DownloadManagerPermissions
import ir.hrka.hooshmand.ai_chat.impl.AiChatUiState
import ir.hrka.hooshmand.ai_chat.impl.ModelDownloadProgress
import ir.hrka.hooshmand.ai_chat.impl.R
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

@Composable
internal fun ModelDownloadDialog(
    uiState: AiChatUiState,
    onStorageLocationSelected: (DownloadStorageLocation) -> Unit,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancel: () -> Unit,
    onPermissionDenied: (String) -> Unit,
    onClearError: () -> Unit,
) {
    val context = LocalContext.current
    val deniedNotificationsMessage =
        stringResource(R.string.model_download_permission_denied_notifications)
    val deniedStorageMessage =
        stringResource(R.string.model_download_permission_denied_storage)
    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    val storageSettingsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            if (!pendingStartAfterPermission) return@rememberLauncherForActivityResult

            if (
                DownloadManagerPermissions.hasAllPermissions(
                    context,
                    uiState.selectedStorageLocation,
                )
            ) {
                pendingStartAfterPermission = false
                onStartDownload()
            } else {
                pendingStartAfterPermission = false
                onPermissionDenied(deniedStorageMessage)
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!pendingStartAfterPermission) return@rememberLauncherForActivityResult

            if (!granted) {
                pendingStartAfterPermission = false
                onPermissionDenied(deniedNotificationsMessage)
                return@rememberLauncherForActivityResult
            }

            val storageLocation = uiState.selectedStorageLocation
            when {
                DownloadManagerPermissions.hasAllPermissions(context, storageLocation) -> {
                    pendingStartAfterPermission = false
                    onStartDownload()
                }
                DownloadManagerPermissions.needsManageExternalStorage(storageLocation) -> {
                    pendingStartAfterPermission = true
                    storageSettingsLauncher.launch(
                        DownloadManagerPermissions.createManageExternalStorageIntent(context),
                    )
                }
                else -> {
                    pendingStartAfterPermission = false
                    onStartDownload()
                }
            }
        }

    fun tryStartDownload() {
        onClearError()
        val storageLocation = uiState.selectedStorageLocation
        when {
            DownloadManagerPermissions.hasAllPermissions(context, storageLocation) -> {
                onStartDownload()
            }
            DownloadManagerPermissions.needsPostNotificationsPermission(context) -> {
                pendingStartAfterPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            DownloadManagerPermissions.needsManageExternalStorage(storageLocation) -> {
                pendingStartAfterPermission = true
                storageSettingsLauncher.launch(
                    DownloadManagerPermissions.createManageExternalStorageIntent(context),
                )
            }
            else -> {
                onStartDownload()
            }
        }
    }

    ModelDownloadDialogContent(
        uiState = uiState,
        onStorageLocationSelected = onStorageLocationSelected,
        onStartDownloadClick = { tryStartDownload() },
        onPauseDownloadClick = onPauseDownload,
        onResumeDownloadClick = onResumeDownload,
        onCancel = onCancel,
    )
}

@Composable
internal fun ModelDownloadDialogContent(
    uiState: AiChatUiState,
    onStorageLocationSelected: (DownloadStorageLocation) -> Unit,
    onStartDownloadClick: () -> Unit,
    onPauseDownloadClick: () -> Unit,
    onResumeDownloadClick: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!uiState.isDownloading) {
                onCancel()
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = !uiState.isDownloading,
                dismissOnClickOutside = !uiState.isDownloading,
            ),
        title = {
            ModelDownloadDialogTitle()
        },
        text = {
            ModelDownloadDialogBody(
                uiState = uiState,
                onStorageLocationSelected = onStorageLocationSelected,
            )
        },
        confirmButton = {
            ModelDownloadDialogConfirmButton(
                uiState = uiState,
                onStartDownloadClick = onStartDownloadClick,
                onPauseDownloadClick = onPauseDownloadClick,
                onResumeDownloadClick = onResumeDownloadClick,
            )
        },
        dismissButton = {
            ModelDownloadDialogCancelButton(onClick = onCancel)
        },
    )
}

@Composable
private fun ModelDownloadDialogTitle() {
    Text(text = stringResource(R.string.model_download_dialog_title))
}

@Composable
private fun ModelDownloadDialogConfirmButton(
    uiState: AiChatUiState,
    onStartDownloadClick: () -> Unit,
    onPauseDownloadClick: () -> Unit,
    onResumeDownloadClick: () -> Unit,
) {
    when {
        uiState.isPaused -> {
            TextButton(onClick = onResumeDownloadClick) {
                Text(text = stringResource(R.string.model_download_resume))
            }
        }
        uiState.isDownloading -> {
            TextButton(onClick = onPauseDownloadClick) {
                Text(text = stringResource(R.string.model_download_pause))
            }
        }
        else -> {
            TextButton(onClick = onStartDownloadClick) {
                Text(text = stringResource(R.string.model_download_start))
            }
        }
    }
}

@Composable
private fun ModelDownloadDialogCancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = stringResource(R.string.model_download_cancel))
    }
}

@Composable
private fun ModelDownloadDialogBody(
    uiState: AiChatUiState,
    onStorageLocationSelected: (DownloadStorageLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.model_download_dialog_message),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(R.string.model_download_storage_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        StorageLocationOption(
            title = stringResource(R.string.model_download_storage_internal_title),
            description = stringResource(R.string.model_download_storage_internal_desc),
            selected = uiState.selectedStorageLocation == DownloadStorageLocation.Internal,
            enabled = !uiState.isDownloading,
            onClick = { onStorageLocationSelected(DownloadStorageLocation.Internal) },
        )
        StorageLocationOption(
            title = stringResource(R.string.model_download_storage_public_title),
            description = stringResource(R.string.model_download_storage_public_desc),
            selected = uiState.selectedStorageLocation == DownloadStorageLocation.Public,
            enabled = !uiState.isDownloading,
            onClick = { onStorageLocationSelected(DownloadStorageLocation.Public) },
        )

        if (uiState.isDownloading) {
            ModelDownloadProgressSection(
                progress = uiState.downloadProgress,
                isPaused = uiState.isPaused,
            )
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ModelDownloadProgressSection(
    progress: ModelDownloadProgress?,
    isPaused: Boolean,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp,
            ),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    stringResource(
                        if (isPaused) {
                            R.string.model_download_paused_label
                        } else {
                            R.string.model_download_progress_label
                        },
                    ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (progress != null) {
                if (progress.totalParts > 1) {
                    Text(
                        text =
                            stringResource(
                                R.string.model_download_part_format,
                                progress.currentPartIndex + 1,
                                progress.totalParts,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                val progressValue = if (progress.progress >= 0f) progress.progress else 0f
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Text(
                    text = formatDownloadProgress(progress),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                if (progress.downloadRateBytesPerSec > 0L) {
                    Text(
                        text =
                            stringResource(
                                R.string.model_download_rate_format,
                                formatBytes(progress.downloadRateBytesPerSec),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Text(
                    text = stringResource(R.string.model_download_progress_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Text(
                text = stringResource(R.string.model_download_notification_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun StorageLocationOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatDownloadProgress(progress: ModelDownloadProgress): String {
    val percentText =
        if (progress.progress >= 0f) {
            stringResource(
                R.string.model_download_percent_format,
                (progress.progress * 100).toInt(),
            )
        } else {
            ""
        }
    val sizeText =
        stringResource(
            R.string.model_download_size_format,
            formatBytes(progress.receivedBytes),
            formatBytes(progress.totalBytes),
        )
    return if (percentText.isNotEmpty()) "$percentText · $sizeText" else sizeText
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}


private fun previewUiState(
    selectedStorageLocation: DownloadStorageLocation = DownloadStorageLocation.Internal,
    isDownloading: Boolean = false,
    downloadProgress: ModelDownloadProgress? = null,
    errorMessage: String? = null,
): AiChatUiState =
    AiChatUiState(
        isCheckingModel = false,
        showDownloadDialog = true,
        selectedStorageLocation = selectedStorageLocation,
        isDownloading = isDownloading,
        downloadProgress = downloadProgress,
        errorMessage = errorMessage,
    )

/**
 * Preview-friendly host. [AlertDialog] renders in a separate window and is often blank
 * in Android Studio previews, so previews use this [Column]-based layout instead.
 */
@Composable
private fun ModelDownloadDialogPreviewHost(
    uiState: AiChatUiState,
    onStorageLocationSelected: (DownloadStorageLocation) -> Unit = {},
    onStartDownloadClick: () -> Unit = {},
    onPauseDownloadClick: () -> Unit = {},
    onResumeDownloadClick: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ModelDownloadDialogContent(
                uiState = uiState,
                onStorageLocationSelected = onStorageLocationSelected,
                onStartDownloadClick = onStartDownloadClick,
                onPauseDownloadClick = onPauseDownloadClick,
                onResumeDownloadClick = onResumeDownloadClick,
                onCancel = onCancel,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Internal storage selected")
@Composable
private fun ModelDownloadDialogInternalStoragePreview() {
    ModelDownloadDialogPreviewHost(
        uiState = previewUiState(selectedStorageLocation = DownloadStorageLocation.Internal),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Public storage selected")
@Composable
private fun ModelDownloadDialogPublicStoragePreview() {
    ModelDownloadDialogPreviewHost(
        uiState = previewUiState(selectedStorageLocation = DownloadStorageLocation.Public),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Downloading – 0%")
@Composable
private fun ModelDownloadDialogDownloadingStartPreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
                isDownloading = true,
                downloadProgress =
                    ModelDownloadProgress(
                        receivedBytes = 0L,
                        totalBytes = 1024L * 1024 * 1024,
                        progress = 0f,
                    ),
            ),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Downloading – 50%")
@Composable
private fun ModelDownloadDialogDownloadingHalfPreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
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
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Downloading – 95%")
@Composable
private fun ModelDownloadDialogDownloadingAlmostDonePreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
                isDownloading = true,
                downloadProgress =
                    ModelDownloadProgress(
                        receivedBytes = (1024L * 1024 * 1024 * 0.95).toLong(),
                        totalBytes = 1024L * 1024 * 1024,
                        progress = 0.95f,
                        remainingTimeMs = 15_000L,
                        downloadRateBytesPerSec = 5_500_000L,
                        currentPartIndex = 4,
                        totalParts = 5,
                    ),
            ),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Downloading – unknown progress")
@Composable
private fun ModelDownloadDialogDownloadingUnknownProgressPreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
                isDownloading = true,
                downloadProgress = null,
            ),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Error")
@Composable
private fun ModelDownloadDialogErrorPreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
                errorMessage = "Failed to connect to the server. Check your internet connection.",
            ),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Permission denied")
@Composable
private fun ModelDownloadDialogPermissionDeniedPreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
                errorMessage = "Cannot start download. Notification permission is required to show download progress.",
            ),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Error while downloading")
@Composable
private fun ModelDownloadDialogDownloadingWithErrorPreview() {
    ModelDownloadDialogPreviewHost(
        uiState =
            previewUiState(
                isDownloading = true,
                downloadProgress =
                    ModelDownloadProgress(
                        receivedBytes = 256L * 1024 * 1024,
                        totalBytes = 1024L * 1024 * 1024,
                        progress = 0.25f,
                    ),
                errorMessage = "Connection interrupted. Download will resume when possible.",
            ),
    )
}
