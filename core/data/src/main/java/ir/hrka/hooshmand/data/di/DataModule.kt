package ir.hrka.hooshmand.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hrka.hooshmand.data.repository.ChangelogRepository
import ir.hrka.hooshmand.data.repository.DefaultChangelogRepository
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
}
