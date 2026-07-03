package ir.hrka.download.manager.api

import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.internal.work.DownloadWorkerFactory

/**
 * Documents Android integration steps that the **host application** is still responsible for.
 *
 * Permissions, [DownloadActionReceiver], foreground-service metadata, and cleartext network
 * configuration are declared in this module's `AndroidManifest.xml` and merged into the host app
 * when the dependency is added.
 *
 * The host app must:
 * 1. Register [DownloadWorkerFactory] in its `Application` via `Configuration.Provider`.
 * 2. Request runtime permissions through [DownloadManagerPermissions] before starting downloads.
 */
object DownloadManagerHostRequirements {

    /** Declared in the library manifest and merged into the host app. */
    const val NOTIFICATION_RECEIVER_CLASS: String =
        "ir.hrka.download.manager.api.DownloadActionReceiver"

    /** Declared in the library manifest and merged into the host app. */
    const val PERMISSION_INTERNET: String = "android.permission.INTERNET"

    /** Declared in the library manifest and merged into the host app. */
    const val PERMISSION_ACCESS_NETWORK_STATE: String =
        "android.permission.ACCESS_NETWORK_STATE"

    /** Declared in the library manifest and merged into the host app. */
    const val PERMISSION_FOREGROUND_SERVICE: String = "android.permission.FOREGROUND_SERVICE"

    /** Declared in the library manifest and merged into the host app. */
    const val PERMISSION_FOREGROUND_SERVICE_DATA_SYNC: String =
        "android.permission.FOREGROUND_SERVICE_DATA_SYNC"

    /**
     * Declared in the library manifest. The host app must request this at runtime on Android 13+
     * before starting a download.
     */
    const val PERMISSION_POST_NOTIFICATIONS: String = "android.permission.POST_NOTIFICATIONS"

    /**
     * Declared in the library manifest for [DownloadStorageLocation.Public].
     * The host app must obtain all-files access at runtime before using public storage.
     */
    const val PERMISSION_MANAGE_EXTERNAL_STORAGE: String =
        "android.permission.MANAGE_EXTERNAL_STORAGE"

    /**
     * Register [DownloadWorkerFactory] in the host `Application` via `Configuration.Provider`:
     * ```
     * override val workManagerConfiguration: Configuration
     *     get() = Configuration.Builder()
     *         .setWorkerFactory(DownloadWorkerFactory())
     *         .build()
     * ```
     */
    val workerFactoryClass: Class<*> = DownloadWorkerFactory::class.java
}
