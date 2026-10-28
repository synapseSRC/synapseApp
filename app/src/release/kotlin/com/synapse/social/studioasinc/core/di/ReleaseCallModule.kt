package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.data.call.CallSimulationController
import com.synapse.social.studioasinc.data.call.NoOpCallSimulationController
import com.synapse.social.studioasinc.data.call.WebRtcCallRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Production calling stack: the WebRTC repository is the only thing that can drive a call.
 *
 * This lives in `app/src/release` and `DebugCallModule` lives in `app/src/debug` because a build variant
 * must contain exactly one binding for [CallRepository], and the debug build wants a different one. Hilt
 * has no conditional binding, and the `@Replaces` annotation the debug module would otherwise use is not
 * part of the Hilt 2.60.1 API surface, so the split is done the way Gradle supports natively: each build
 * type contributes its own module and neither variant ever sees the other's. A release binary therefore
 * cannot contain the simulator even by accident — the source set that declares it is not compiled in.
 *
 * Everything else the call needs (`CallDeviceModule`, `CallUseCaseModule`, `CallBackendModule`,
 * `WebRtcModule`) stays in the main source set, installed in both variants, which is the point of the
 * arrangement: a simulated call runs through the same UseCase preconditions, the same state machine and
 * the same audio hardware as a real one, so what the debug build proves about the UI transfers to release.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReleaseCallModule {

    @Provides
    @Singleton
    fun provideCallRepository(repository: WebRtcCallRepository): CallRepository = repository

    /**
     * Nothing can simulate a production call, so nothing is listening.
     *
     * The debug sheet still renders and still sends its actions; they land here and stop, which is the
     * honest behaviour for a build that has no simulated repository to feed them to.
     */
    @Provides
    @Singleton
    fun provideCallSimulationController(): CallSimulationController = NoOpCallSimulationController
}
