package ir.hrka.download.manager.api

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import ir.hrka.download.manager.DownloadStorageLocation

/**
 * Runtime permission helpers for [DownloadManager].
 *
 * Manifest permissions and the [DownloadActionReceiver] are declared in the library
 * `AndroidManifest.xml` and merged into the host app automatically. The host app is still
 * responsible for requesting runtime permissions before starting downloads.
 *
 * @see DownloadManagerHostRequirements
 */
object DownloadManagerPermissions {

    const val DENIED_POST_NOTIFICATIONS_MESSAGE: String =
        "Notification permission is required to show download progress and controls."

    const val DENIED_MANAGE_EXTERNAL_STORAGE_MESSAGE: String =
        "All files access is required for Public storage. Enable it in Settings, then try again."

    fun needsPostNotificationsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED

    fun needsManageExternalStorage(storageLocation: DownloadStorageLocation): Boolean =
        storageLocation == DownloadStorageLocation.Public &&
            !Environment.isExternalStorageManager()

    fun hasAllPermissions(
        context: Context,
        storageLocation: DownloadStorageLocation,
    ): Boolean = !needsPostNotificationsPermission(context) &&
        !needsManageExternalStorage(storageLocation)

    fun createManageExternalStorageIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun permissionHint(storageLocation: DownloadStorageLocation): String? =
        when (storageLocation) {
            DownloadStorageLocation.Public ->
                "Public storage requires “All files access” in system Settings."
            DownloadStorageLocation.Internal,
            DownloadStorageLocation.External,
            -> null
        }
}
