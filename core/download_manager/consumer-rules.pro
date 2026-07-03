# Keep public API surface for consumers.
-keep public class ir.hrka.download.manager.api.** { *; }

# WorkManager must be able to instantiate the download worker.
-keep class ir.hrka.download.manager.internal.work.DownloadWorker { *; }
-keep class ir.hrka.download.manager.internal.work.DownloadWorkerFactory { *; }
