package ir.hrka.download.manager.internal.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ir.hrka.download.manager.DownloadStatus
import ir.hrka.download.manager.api.DownloadActionReceiver
import ir.hrka.download.manager.internal.work.DownloadWorkActions
import ir.hrka.download.manager.internal.work.DownloadWorkInput
import ir.hrka.download.manager.internal.work.DownloadWorkProgress

/**
 * Builds and updates foreground download notifications with progress and control actions.
 *
 * Used by [ir.hrka.download.manager.internal.work.DownloadWorker] to satisfy WorkManager
 * foreground-service requirements. Notifications expose pause, resume, and stop actions
 * that dispatch to [DownloadActionReceiver].
 *
 * The library manifest registers [DownloadActionReceiver]; see [DownloadManagerHostRequirements].
 *
 * @property context Application [Context] for system services and pending intents.
 * @property downloadId Stable job identifier passed to control actions.
 * @property fileName Display name shown as the notification title.
 *
 * @see DownloadWorkProgress
 * @see DownloadActionReceiver
 */
internal class DownloadNotificationHelper(
    private val context: Context,
    private val downloadId: String,
    private val fileName: String,
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Stable notification id derived from [downloadId].
     *
     * Used by [ir.hrka.download.manager.internal.work.DownloadWorker] when calling [androidx.work.ForegroundInfo].
     */
    val notificationId: Int = downloadId.hashCode()

    init {
        createChannelIfNeeded()
    }

    /**
     * Builds a notification reflecting the current [progress] snapshot.
     *
     * Includes a determinate or indeterminate progress bar, status text, and contextual
     * actions (pause/resume/stop) based on [DownloadWorkProgress.status].
     *
     * @param progress Current download progress and lifecycle state.
     * @return A notification ready for [androidx.work.ForegroundInfo].
     */
    fun buildNotification(progress: DownloadWorkProgress): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText(buildContentText(progress))
            .setOngoing(isOngoing(progress.status))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)

        if (progress.progress in 0f..1f) {
            builder.setProgress(100, (progress.progress * 100).toInt(), false)
        } else {
            builder.setProgress(0, 0, true)
        }

        addActions(builder, progress.status)
        return builder.build()
    }

    /** Returns user-visible status line for the notification body. */
    private fun buildContentText(progress: DownloadWorkProgress): String = when (progress.status) {
        DownloadStatus.Pending -> "Waiting to start…"
        DownloadStatus.Downloading -> buildDownloadingText(progress)
        DownloadStatus.Paused -> "Paused"
        DownloadStatus.Completed -> "Download complete"
        DownloadStatus.Canceled -> "Download canceled"
        DownloadStatus.Failed -> progress.errorMessage ?: "Download failed"
        DownloadStatus.Merging -> "Processing…"
    }

    /**
     * Formats active download text with optional multipart prefix, bytes transferred, and speed.
     *
     * Example: `Part 2/5 · 12.5 MB / 50.0 MB · 1024 KB/s`
     */
    private fun buildDownloadingText(progress: DownloadWorkProgress): String {
        val speedKb = progress.downloadRate / 1024
        val partText = if (progress.totalParts > 1) {
            "Part ${progress.currentPartIndex + 1}/${progress.totalParts} · "
        } else {
            ""
        }
        val percentText = if (progress.progress in 0f..1f) {
            "${(progress.progress * 100).toInt()}% · "
        } else {
            ""
        }
        return "${percentText}${partText}${formatBytes(progress.receivedBytes)}${progress.totalBytes?.let { " / ${formatBytes(it)}" } ?: ""} · ${speedKb} KB/s"
    }

    /**
     * Adds pause/resume/stop actions appropriate for [status].
     *
     * @param builder Notification builder to mutate.
     * @param status Current download lifecycle state.
     */
    private fun addActions(
        builder: NotificationCompat.Builder,
        status: DownloadStatus,
    ) {
        when (status) {
            DownloadStatus.Downloading -> {
                builder.addAction(0, "Pause", actionPendingIntent(DownloadWorkActions.ACTION_PAUSE))
                builder.addAction(0, "Stop", actionPendingIntent(DownloadWorkActions.ACTION_STOP))
            }

            DownloadStatus.Paused -> {
                builder.addAction(0, "Resume", actionPendingIntent(DownloadWorkActions.ACTION_RESUME))
                builder.addAction(0, "Stop", actionPendingIntent(DownloadWorkActions.ACTION_STOP))
            }

            else -> Unit
        }
    }

    /**
     * Creates a broadcast [PendingIntent] for a notification control [action].
     *
     * @param action One of [DownloadWorkActions.ACTION_PAUSE], [DownloadWorkActions.ACTION_RESUME],
     * or [DownloadWorkActions.ACTION_STOP].
     */
    private fun actionPendingIntent(action: String): PendingIntent {
        val intent =
            Intent(context, DownloadActionReceiver::class.java).apply {
                setAction(action)
                setPackage(context.packageName)
                putExtra(DownloadWorkActions.EXTRA_DOWNLOAD_ID, downloadId)
            }
        val requestCode =
            when (action) {
                DownloadWorkActions.ACTION_PAUSE -> REQUEST_CODE_PAUSE
                DownloadWorkActions.ACTION_RESUME -> REQUEST_CODE_RESUME
                DownloadWorkActions.ACTION_STOP -> REQUEST_CODE_STOP
                else -> action.hashCode()
            }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            downloadId.hashCode() + requestCode,
            intent,
            flags,
        )
    }

    /** Returns whether the notification should remain ongoing (non-dismissible) for [status]. */
    private fun isOngoing(status: DownloadStatus): Boolean =
        status == DownloadStatus.Pending ||
            status == DownloadStatus.Downloading ||
            status == DownloadStatus.Paused ||
            status == DownloadStatus.Merging

    /** Creates the low-importance download notification channel on API 26+. */
    private fun createChannelIfNeeded() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Download progress and controls"
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Formats a byte count as a human-readable size string. */
    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

    companion object {
        /** Notification channel id for all download jobs. */
        private const val CHANNEL_ID = "hrka_download_channel"

        /** User-visible notification channel name. */
        private const val CHANNEL_NAME = "Downloads"

        private const val REQUEST_CODE_PAUSE = 1
        private const val REQUEST_CODE_RESUME = 2
        private const val REQUEST_CODE_STOP = 3

        /**
         * Creates a helper bound to the file and job id from [input].
         *
         * @param context Application [Context].
         * @param input Work input containing [DownloadWorkInput.downloadId] and [DownloadWorkInput.fileName].
         */
        fun fromInput(context: Context, input: DownloadWorkInput): DownloadNotificationHelper =
            DownloadNotificationHelper(
                context = context,
                downloadId = input.downloadId,
                fileName = input.fileName,
            )
    }
}
