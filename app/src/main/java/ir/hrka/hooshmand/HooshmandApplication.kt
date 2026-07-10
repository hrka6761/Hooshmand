package ir.hrka.hooshmand

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.hrka.download.manager.internal.work.DownloadWorkerFactory

@HiltAndroidApp
class HooshmandApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(DownloadWorkerFactory())
                .build()
}