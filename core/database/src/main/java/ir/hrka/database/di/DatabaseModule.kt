package ir.hrka.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hrka.database.HooshmandDatabase
import ir.hrka.database.dao.PlaceholderDao
import javax.inject.Singleton

/**
 * Provides the singleton [HooshmandDatabase] instance.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    /**
     * Builds the app Room database file `hooshmand-database`.
     *
     * @param context Application context.
     * @return Configured [HooshmandDatabase].
     */
    @Provides
    @Singleton
    fun providesHooshmandDatabase(
        @ApplicationContext context: Context,
    ): HooshmandDatabase =
        Room.databaseBuilder(
            context,
            HooshmandDatabase::class.java,
            "hooshmand-database",
        ).build()
}

/**
 * Provides Room DAOs from [HooshmandDatabase].
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {

    /**
     * @param database App Room database.
     * @return [PlaceholderDao] for the temporary scaffold table.
     */
    @Provides
    fun providesPlaceholderDao(
        database: HooshmandDatabase,
    ): PlaceholderDao = database.placeholderDao()
}
