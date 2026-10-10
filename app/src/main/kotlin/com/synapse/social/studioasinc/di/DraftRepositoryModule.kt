package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.data.repository.DraftRepositoryImpl
import com.synapse.social.studioasinc.domain.repository.DraftRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DraftRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDraftRepository(
        draftRepositoryImpl: DraftRepositoryImpl
    ): DraftRepository
}
