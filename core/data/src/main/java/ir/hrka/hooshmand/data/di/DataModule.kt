package ir.hrka.hooshmand.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hrka.hooshmand.data.repository.ChangelogRepository
import ir.hrka.hooshmand.data.repository.ChatHistoryRepository
import ir.hrka.hooshmand.data.repository.DefaultChangelogRepository
import ir.hrka.hooshmand.data.repository.DefaultChatHistoryRepository
import ir.hrka.hooshmand.data.repository.DefaultModelManifestRepository
import ir.hrka.hooshmand.data.repository.ModelManifestRepository
import javax.inject.Singleton

/**
 * Hilt bindings for the data layer public API.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindChangelogRepository(impl: DefaultChangelogRepository): ChangelogRepository

    @Binds
    @Singleton
    abstract fun bindChatHistoryRepository(impl: DefaultChatHistoryRepository): ChatHistoryRepository

    @Binds
    @Singleton
    abstract fun bindModelManifestRepository(
        impl: DefaultModelManifestRepository,
    ): ModelManifestRepository
}
