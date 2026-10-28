package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.data.call.CallRealtimeDataSource
import com.synapse.social.studioasinc.data.call.CallSessionCoordinator
import com.synapse.social.studioasinc.data.call.CallSessionDataSource
import com.synapse.social.studioasinc.data.call.SupabaseCallSessionRepository
import com.synapse.social.studioasinc.data.call.TurnCredentialRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository
import com.synapse.social.studioasinc.shared.domain.repository.IncomingCallWatcher
import com.synapse.social.studioasinc.shared.domain.repository.TurnCredentialRepository
import com.synapse.social.studioasinc.shared.domain.usecase.call.DeclineCallInvitationUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.MintTurnCredentialsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import javax.inject.Singleton

/**
 * Backend half of the calling stack: the session state machine, the signaling transport, and
 * the traversal credentials.
 *
 * Separate from the modules that bind the repository the screen talks to (`ReleaseCallModule`,
 * `DebugCallModule`). This module has no opinion about which implementation of `CallRepository` is
 * installed — it only supplies the facts the real one needs. Both data sources are singletons
 * because state that lives in them
 * must outlive one subscription: `CallRealtimeDataSource` counts the signaling sequence numbers
 * a session has published, and losing that count mid-call would collide with rows already stored
 * and drop a negotiation step.
 */
@Module
@InstallIn(SingletonComponent::class)
object CallBackendModule {

    @Provides
    @Singleton
    fun provideCallSessionDataSource(client: SupabaseClientType): CallSessionDataSource =
        CallSessionDataSource(client)

    @Provides
    @Singleton
    fun provideCallRealtimeDataSource(client: SupabaseClientType): CallRealtimeDataSource =
        CallRealtimeDataSource(client)

    @Provides
    @Singleton
    fun provideCallSessionRepository(
        sessions: CallSessionDataSource,
        realtime: CallRealtimeDataSource
    ): CallSessionRepository = SupabaseCallSessionRepository(sessions, realtime)

    @Provides
    @Singleton
    fun provideTurnCredentialRepository(client: SupabaseClientType): TurnCredentialRepository =
        TurnCredentialRepositoryImpl(client)

    /**
     * The process-wide ring watch.
     *
     * Singleton because a ring has to outlive whatever screen noticed it: the notification it posts has
     * to be withdrawable when that screen goes away, and two subscriptions would ring twice.
     */
    @Provides
    @Singleton
    fun provideIncomingCallWatcher(coordinator: CallSessionCoordinator): IncomingCallWatcher = coordinator

    /**
     * The decline that needs no local call.
     *
     * `DeclineCallUseCase` goes through `CallRepository` because declining a call this device is holding
     * also means releasing its hardware. The notification shade's decline has no hardware to release, so
     * it writes the transition to the ledger directly — which is also the only version that works when the
     * app was closed when the call arrived.
     */
    @Provides
    fun provideDeclineCallInvitationUseCase(
        callSessionRepository: CallSessionRepository
    ): DeclineCallInvitationUseCase = DeclineCallInvitationUseCase(callSessionRepository)

    @Provides
    @Singleton
    fun provideMintTurnCredentialsUseCase(
        turnCredentialRepository: TurnCredentialRepository
    ): MintTurnCredentialsUseCase = MintTurnCredentialsUseCase(turnCredentialRepository)
}
