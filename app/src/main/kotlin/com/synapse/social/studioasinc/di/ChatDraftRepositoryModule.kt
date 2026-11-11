package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.data.repository.ChatDraftRepositoryImpl
import com.synapse.social.studioasinc.domain.repository.ChatDraftRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatDraftRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChatDraftRepository(
        chatDraftRepositoryImpl: ChatDraftRepositoryImpl
    ): ChatDraftRepository
}
