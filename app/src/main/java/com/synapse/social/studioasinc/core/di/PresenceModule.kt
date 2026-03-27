package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.data.repository.SupabasePresenceRepository
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserActiveStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.StartPresenceTrackingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdatePresenceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdateCurrentChatUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PresenceModule {
    
    @Provides
    @Singleton
    fun providePresenceRepository(): PresenceRepository = SupabasePresenceRepository()
    
    @Provides
    @Singleton
    fun provideUpdatePresenceUseCase(repository: PresenceRepository) = UpdatePresenceUseCase(repository)
    
    @Provides
    @Singleton
    fun provideStartPresenceTrackingUseCase(repository: PresenceRepository) = StartPresenceTrackingUseCase(repository)
    
    @Provides
    @Singleton
    fun provideObserveUserPresenceUseCase(repository: PresenceRepository) = ObserveUserPresenceUseCase(repository)
    
    @Provides
    @Singleton
    fun provideObserveUserActiveStatusUseCase(repository: PresenceRepository) = ObserveUserActiveStatusUseCase(repository)
    
    @Provides
    @Singleton
    fun provideUpdateCurrentChatUseCase(repository: PresenceRepository) = UpdateCurrentChatUseCase(repository)
}
