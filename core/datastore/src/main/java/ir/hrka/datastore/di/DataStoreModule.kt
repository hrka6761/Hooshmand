package ir.hrka.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hrka.datastore.PrimitivePreferencesDataSource
import ir.hrka.datastore.api.PrimitiveDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Qualifier for the app-wide preferences DataStore instance. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HrkaPreferencesDataStore

/**
 * Hilt wiring for Preferences DataStore.
 *
 * The raw `DataStore` is internal to this module while [PrimitiveDataStore] is the public facade.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreBindingsModule {

    @Binds
    @Singleton
    abstract fun bindPrimitiveDataStore(
        dataSource: PrimitivePreferencesDataSource,
    ): PrimitiveDataStore
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {

    private const val PREFERENCES_FILE_NAME = "hrka_preferences.preferences_pb"

    @Provides
    @Singleton
    @HrkaPreferencesDataStore
    fun providesPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(PREFERENCES_FILE_NAME) },
    )
}
