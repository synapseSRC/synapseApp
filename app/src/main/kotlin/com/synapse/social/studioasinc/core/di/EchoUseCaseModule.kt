package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import com.synapse.social.studioasinc.shared.domain.repository.EchoTokenRepository
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine
import com.synapse.social.studioasinc.shared.domain.usecase.echo.CreateEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.EchoHeartbeatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.EndEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.JoinEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.LeaveEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.LoadEchoLobbyUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.ObserveEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.ReconnectEchoAudioUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.SetEchoHandRaisedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.SetEchoRoleUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The Echo Rooms UseCases the lobby and room screens are allowed to talk to.
 *
 * Nothing here is scoped: a UseCase is a constructor over interfaces, and the state that has to
 * outlive a screen lives in the repository and the audio engine.
 */
@Module
@InstallIn(SingletonComponent::class)
object EchoUseCaseModule {

    @Provides
    fun provideLoadEchoLobbyUseCase(repository: EchoRoomRepository): LoadEchoLobbyUseCase =
        LoadEchoLobbyUseCase(repository)

    @Provides
    fun provideCreateEchoRoomUseCase(repository: EchoRoomRepository): CreateEchoRoomUseCase =
        CreateEchoRoomUseCase(repository)

    @Provides
    fun provideEchoHeartbeatUseCase(repository: EchoRoomRepository): EchoHeartbeatUseCase =
        EchoHeartbeatUseCase(repository)

    @Provides
    fun provideSetEchoRoleUseCase(repository: EchoRoomRepository): SetEchoRoleUseCase =
        SetEchoRoleUseCase(repository)

    @Provides
    fun provideSetEchoHandRaisedUseCase(repository: EchoRoomRepository): SetEchoHandRaisedUseCase =
        SetEchoHandRaisedUseCase(repository)

    @Provides
    fun provideObserveEchoRoomUseCase(repository: EchoRoomRepository): ObserveEchoRoomUseCase =
        ObserveEchoRoomUseCase(repository)

    @Provides
    fun provideJoinEchoRoomUseCase(
        repository: EchoRoomRepository,
        tokenRepository: EchoTokenRepository,
        audioEngine: EchoAudioEngine
    ): JoinEchoRoomUseCase = JoinEchoRoomUseCase(repository, tokenRepository, audioEngine)

    @Provides
    fun provideLeaveEchoRoomUseCase(
        repository: EchoRoomRepository,
        audioEngine: EchoAudioEngine
    ): LeaveEchoRoomUseCase = LeaveEchoRoomUseCase(repository, audioEngine)

    @Provides
    fun provideEndEchoRoomUseCase(
        repository: EchoRoomRepository,
        audioEngine: EchoAudioEngine
    ): EndEchoRoomUseCase = EndEchoRoomUseCase(repository, audioEngine)

    @Provides
    fun provideReconnectEchoAudioUseCase(
        tokenRepository: EchoTokenRepository,
        audioEngine: EchoAudioEngine
    ): ReconnectEchoAudioUseCase = ReconnectEchoAudioUseCase(tokenRepository, audioEngine)
}
