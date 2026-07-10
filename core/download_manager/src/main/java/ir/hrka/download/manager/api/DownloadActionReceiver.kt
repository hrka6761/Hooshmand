package ir.hrka.download.manager.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import ir.hrka.download.manager.internal.work.DownloadWorkActions
import ir.hrka.download.manager.internal.work.DownloadWorkerControl

/**
 * Handles pause, resume, and stop actions from the download foreground notification.
 *
 * Registered in the library `AndroidManifest.xml` and merged into the host application.
 *
 * @see DownloadManagerHostRequirements
 */
class DownloadActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val pendingResult = goAsync()
        val downloadId = intent.getStringExtra(DownloadWorkActions.EXTRA_DOWNLOAD_ID)
        if (downloadId.isNullOrBlank()) {
            pendingResult.finish()
            return
        }

        when (intent.action) {
            DownloadWorkActions.ACTION_PAUSE -> DownloadWorkerControl.requestPause(downloadId)
            DownloadWorkActions.ACTION_RESUME -> DownloadWorkerControl.requestResume(downloadId)
            DownloadWorkActions.ACTION_STOP -> {
                DownloadWorkerControl.requestCancel(downloadId)
                WorkManager.getInstance(context.applicationContext).cancelUniqueWork(downloadId)
            }
        }
        pendingResult.finish()
    }
}
