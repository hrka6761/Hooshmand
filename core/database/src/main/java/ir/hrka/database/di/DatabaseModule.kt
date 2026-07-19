package ir.hrka.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hrka.database.HooshmandDatabase
import ir.hrka.database.dao.ConversationDao
import ir.hrka.database.dao.MessageDao
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
     * Uses destructive migration for pre-release schema resets (placeholder → chat tables).
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
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}

/**
 * Provides Room DAOs from [HooshmandDatabase].
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {

    /**
     * @param database App Room database.
     * @return [ConversationDao] for conversation rows.
     */
    @Provides
    fun providesConversationDao(
        database: HooshmandDatabase,
    ): ConversationDao = database.conversationDao()

    /**
     * @param database App Room database.
     * @return [MessageDao] for message rows.
     */
    @Provides
    fun providesMessageDao(
        database: HooshmandDatabase,
    ): MessageDao = database.messageDao()
}
