# Keep public API surface for consumers.
-keep public class ir.hrka.download.manager.api.** { *; }
-keep public class ir.hrka.download.manager.model.** { *; }
-keep public class ir.hrka.download.manager.error.** { *; }

# WorkManager must be able to instantiate the download worker.
-keep class ir.hrka.download.manager.internal.work.DownloadWorker { *; }
