package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.core.media.AndroidSoundPlayer
import com.synapse.social.studioasinc.data.repository.SoundSettingsRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.SoundSettingsRepository
import com.synapse.social.studioasinc.shared.domain.service.SoundPlayer
import com.synapse.social.studioasinc.shared.domain.usecase.sound.PlaySoundEffectUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.sound.ResolveCallSoundUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The sound-effect seam: one platform player, one preference store, and the two UseCases that decide
 * what gets heard. Consumers take the domain types, never [AndroidSoundPlayer], except the call audio
 * router — which is the platform half of the same problem and has to say when the device is in a call.
 */
@Module
@InstallIn(SingletonComponent::class)
object SoundModule {

    @Provides
    @Singleton
    fun provideSoundPlayer(player: AndroidSoundPlayer): SoundPlayer = player

    @Provides
    @Singleton
    fun provideSoundSettingsRepository(repository: SoundSettingsRepositoryImpl): SoundSettingsRepository =
        repository

    @Provides
    @Singleton
    fun providePlaySoundEffectUseCase(
        player: SoundPlayer,
        settings: SoundSettingsRepository
    ): PlaySoundEffectUseCase = PlaySoundEffectUseCase(player, settings)

    @Provides
    @Singleton
    fun provideResolveCallSoundUseCase(): ResolveCallSoundUseCase = ResolveCallSoundUseCase()
}
