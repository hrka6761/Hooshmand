package ir.hrka.hooshmand.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hrka.hooshmand.network.ChangelogNetworkDataSource
import ir.hrka.hooshmand.network.ModelManifestNetworkDataSource
import ir.hrka.hooshmand.network.github.GitHubChangelogNetworkDataSource
import ir.hrka.hooshmand.network.github.GitHubModelManifestNetworkDataSource
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt bindings for the network layer public API.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {

    @Binds
    @Singleton
    abstract fun bindChangelogNetworkDataSource(
        impl: GitHubChangelogNetworkDataSource,
    ): ChangelogNetworkDataSource

    @Binds
    @Singleton
    abstract fun bindModelManifestNetworkDataSource(
        impl: GitHubModelManifestNetworkDataSource,
    ): ModelManifestNetworkDataSource
}

/**
 * Hilt providers for shared HTTP and JSON infrastructure.
 *
 * The GitHub changelog endpoint is public, so no authorization interceptor is registered.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 10L
    private const val WRITE_TIMEOUT_SECONDS = 10L

    /**
     * Provides a process-wide [Json] configured for GitHub Contents API responses.
     */
    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Provides a process-wide [OkHttpClient] for remote file fetches.
     */
    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
}
