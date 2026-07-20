package ir.hrka.download.manager.internal.transfer

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared [OkHttpClient] for all download transfers in this process.
 *
 * Reusing one client keeps the connection pool warm across single-file and multipart segments.
 */
internal object DownloadHttpClient {

    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
