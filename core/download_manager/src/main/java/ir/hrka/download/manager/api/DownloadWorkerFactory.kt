package ir.hrka.download.manager.api

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ir.hrka.download.manager.internal.work.DownloadWorker

/**
 * WorkManager [WorkerFactory] that creates [DownloadWorker] instances.
 *
 * [DownloadWorker] is internal to the library, so the host app must register this factory
 * when configuring WorkManager. Delegate all other worker types to the default factory.
 *
 * ## Host app setup
 * ```
 * class MyApplication : Application(), Configuration.Provider {
 *
 *     override val workManagerConfiguration: Configuration
 *         get() = Configuration.Builder()
 *             .setWorkerFactory(DownloadWorkerFactory())
 *             .build()
 * }
 * ```
 *
 * If the app already uses a custom [WorkerFactory], wrap or chain factories so
 * [DownloadWorker] is still created for its class name.
 */
class DownloadWorkerFactory : WorkerFactory() {

    /**
     * Creates a [DownloadWorker] when [workerClassName] matches [DownloadWorker].
     *
     * @param appContext Application [Context].
     * @param workerClassName Fully qualified worker class name from the work request.
     * @param workerParameters WorkManager-provided parameters.
     * @return A [DownloadWorker] instance, or `null` if [workerClassName] is not handled.
     */
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == DownloadWorker::class.java.name) {
            DownloadWorker(appContext, workerParameters)
        } else {
            null
        }
}
