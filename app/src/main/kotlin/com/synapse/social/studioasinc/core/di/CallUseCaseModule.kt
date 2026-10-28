package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.repository.CallPermissionRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository
import com.synapse.social.studioasinc.shared.domain.repository.IncomingCallWatcher
import com.synapse.social.studioasinc.shared.domain.usecase.call.AcceptCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.CallHeartbeatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.DeclineCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.EndCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.FlipCallCameraUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.GetCallHistoryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ObserveCallSessionUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ObserveIncomingCallsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.PrepareCallPermissionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.RecordCallPermissionOutcomeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ResolveStaleCallSessionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.SelectCallAudioRouteUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.StartCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.SwitchCallMediaTypeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ToggleCallMuteUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ToggleLocalVideoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The calling UseCases the screen is allowed to talk to.
 *
 * Nothing here is scoped: a UseCase is a constructor over an interface, and the state that has to
 * survive lives in the repositories it wraps. `MintTurnCredentialsUseCase` is provided by
 * `CallBackendModule` instead, because that one belongs to the backend half of the stack rather than
 * to a screen.
 *
 * Like `CallDeviceModule`, this module is *not* replaced in debug builds — a simulated call has to go
 * through exactly the same precondition checks as a real one, or the debug build stops testing the code
 * the release build runs.
 */
@Module
@InstallIn(SingletonComponent::class)
object CallUseCaseModule {

    @Provides
    fun provideStartCallUseCase(callRepository: CallRepository): StartCallUseCase =
        StartCallUseCase(callRepository)

    @Provides
    fun provideAcceptCallUseCase(callRepository: CallRepository): AcceptCallUseCase =
        AcceptCallUseCase(callRepository)

    @Provides
    fun provideDeclineCallUseCase(callRepository: CallRepository): DeclineCallUseCase =
        DeclineCallUseCase(callRepository)

    @Provides
    fun provideEndCallUseCase(callRepository: CallRepository): EndCallUseCase =
        EndCallUseCase(callRepository)

    @Provides
    fun provideObserveCallSessionUseCase(callRepository: CallRepository): ObserveCallSessionUseCase =
        ObserveCallSessionUseCase(callRepository)

    /**
     * The ring a screen may open itself for.
     *
     * Sits here rather than beside the session repository because the thing it decides is *what the app
     * shows*: an unanswered invitation is worth a ringing screen, and everything else is not. The watch
     * behind it is what keeps that to one notification and one subscription per call.
     */
    @Provides
    fun provideObserveIncomingCallsUseCase(watcher: IncomingCallWatcher): ObserveIncomingCallsUseCase =
        ObserveIncomingCallsUseCase(watcher)

    @Provides
    fun provideToggleCallMuteUseCase(callRepository: CallRepository): ToggleCallMuteUseCase =
        ToggleCallMuteUseCase(callRepository)

    @Provides
    fun provideSelectCallAudioRouteUseCase(callRepository: CallRepository): SelectCallAudioRouteUseCase =
        SelectCallAudioRouteUseCase(callRepository)

    @Provides
    fun provideToggleLocalVideoUseCase(callRepository: CallRepository): ToggleLocalVideoUseCase =
        ToggleLocalVideoUseCase(callRepository)

    @Provides
    fun provideFlipCallCameraUseCase(callRepository: CallRepository): FlipCallCameraUseCase =
        FlipCallCameraUseCase(callRepository)

    @Provides
    fun provideSwitchCallMediaTypeUseCase(callRepository: CallRepository): SwitchCallMediaTypeUseCase =
        SwitchCallMediaTypeUseCase(callRepository)

    @Provides
    fun providePrepareCallPermissionsUseCase(
        permissionRepository: CallPermissionRepository
    ): PrepareCallPermissionsUseCase = PrepareCallPermissionsUseCase(permissionRepository)

    @Provides
    fun provideRecordCallPermissionOutcomeUseCase(
        permissionRepository: CallPermissionRepository
    ): RecordCallPermissionOutcomeUseCase = RecordCallPermissionOutcomeUseCase(permissionRepository)

    /** Backs the Calls tab; reads and heals the same session rows the call screen uses. */
    @Provides
    fun provideGetCallHistoryUseCase(sessions: CallSessionRepository): GetCallHistoryUseCase =
        GetCallHistoryUseCase(sessions)

    @Provides
    fun provideResolveStaleCallSessionsUseCase(
        sessions: CallSessionRepository
    ): ResolveStaleCallSessionsUseCase = ResolveStaleCallSessionsUseCase(sessions)

    @Provides
    fun provideCallHeartbeatUseCase(sessions: CallSessionRepository): CallHeartbeatUseCase =
        CallHeartbeatUseCase(sessions)
}
