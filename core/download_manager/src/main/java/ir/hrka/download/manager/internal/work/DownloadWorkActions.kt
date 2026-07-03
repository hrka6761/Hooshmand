package ir.hrka.download.manager.internal.work

/**
 * Intent actions and extras used by download notification controls.
 *
 * Dispatched from [ir.hrka.download.manager.internal.notification.DownloadNotificationHelper]
 * to [DownloadActionReceiver].
 */
internal object DownloadWorkActions {

    /** Broadcast action to pause an active download. */
    const val ACTION_PAUSE = "ir.hrka.download.manager.action.PAUSE"

    /** Broadcast action to resume a paused download. */
    const val ACTION_RESUME = "ir.hrka.download.manager.action.RESUME"

    /** Broadcast action to stop and cancel an active download. */
    const val ACTION_STOP = "ir.hrka.download.manager.action.STOP"

    /**
     * Intent extra key for the target download job id.
     *
     * Value type: [String] matching [DownloadWorkInput.downloadId].
     */
    const val EXTRA_DOWNLOAD_ID = "extra_download_id"
}
