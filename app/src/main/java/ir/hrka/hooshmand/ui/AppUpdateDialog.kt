package ir.hrka.hooshmand.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import ir.hrka.hooshmand.R
import ir.hrka.hooshmand.domain.AppUpdateStatus

/**
 * Dialog prompting the user to update the app from Cafe Bazaar.
 *
 * @param isMandatory `true` for a forced update; back/outside dismiss is disabled.
 * @param onUpdateClick Opens the Cafe Bazaar listing.
 * @param onCancelClick Continues to home (optional) or closes the app (mandatory).
 */
@Composable
fun AppUpdateDialog(
    isMandatory: Boolean,
    onUpdateClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isMandatory) {
                onCancelClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory,
        ),
        title = {
            Text(text = stringResource(R.string.app_update_dialog_title))
        },
        text = {
            Column {
                Text(text = stringResource(R.string.app_update_dialog_message))
                if (isMandatory) {
                    Text(
                        text = stringResource(R.string.app_update_dialog_mandatory_message),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateClick) {
                Text(text = stringResource(R.string.app_update_dialog_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelClick) {
                Text(text = stringResource(R.string.app_update_dialog_cancel))
            }
        },
    )
}

/**
 * Whether [status] should show [AppUpdateDialog].
 */
fun AppUpdateStatus.shouldShowUpdateDialog(): Boolean =
    this is AppUpdateStatus.OptionalUpdate || this is AppUpdateStatus.MandatoryUpdate

/**
 * Whether [status] is a mandatory update.
 */
fun AppUpdateStatus.isMandatoryUpdate(): Boolean =
    this is AppUpdateStatus.MandatoryUpdate
