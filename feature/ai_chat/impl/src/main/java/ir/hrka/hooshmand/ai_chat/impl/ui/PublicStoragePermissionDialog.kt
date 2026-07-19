package ir.hrka.hooshmand.ai_chat.impl.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import android.os.Environment
import ir.hrka.download.manager.api.DownloadManagerPermissions
import ir.hrka.hooshmand.ai_chat.impl.R

/**
 * Prompts for all-files access when a complete model already exists in public storage.
 *
 * The download dialog is skipped in that case, so this dialog owns the permission request
 * that would otherwise happen when starting a public-storage download.
 *
 * @param onPermissionGranted Called when all-files access is granted.
 * @param onPermissionDenied Called with a user-visible message when access is still missing.
 * @param onCancel Called when the user dismisses without granting access.
 * @param modifier Optional [Modifier] for the dialog.
 */
@Composable
internal fun PublicStoragePermissionDialog(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val deniedStorageMessage =
        stringResource(R.string.model_public_storage_permission_denied)

    val storageSettingsLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            // Only all-files access is required to load an existing public model.
            // Notification permission is download-only and must not block inference.
            if (Environment.isExternalStorageManager()) {
                onPermissionGranted()
            } else {
                onPermissionDenied(deniedStorageMessage)
            }
        }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(text = stringResource(R.string.model_public_storage_permission_title))
        },
        text = {
            Text(text = stringResource(R.string.model_public_storage_permission_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    storageSettingsLauncher.launch(
                        DownloadManagerPermissions.createManageExternalStorageIntent(context),
                    )
                },
            ) {
                Text(text = stringResource(R.string.model_public_storage_permission_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.model_download_cancel))
            }
        },
    )
}
