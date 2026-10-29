package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.data.call.CallSimulationController
import com.synapse.social.studioasinc.data.call.MockCallRepository
import com.synapse.social.studioasinc.data.call.NoOpCallSimulationController
import com.synapse.social.studioasinc.data.call.WebRtcCallRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The debug build's calling stack.
 *
 * This module is the whole of the debug half of a two-module split: `app/src/release` contributes
 * `ReleaseCallModule` and this file contributes the same two bindings for the debug variant, so exactly
 * one of them is ever installed and no `@Replaces` is needed — Hilt 2.60.1 does not ship that annotation,
 * and the Gradle build-type source set is the mechanism that keeps the simulator out of release anyway.
 * The other call modules (`CallDeviceModule`, `CallUseCaseModule`, `CallBackendModule`, `WebRtcModule`)
 * live in the main source set and stay installed here on purpose: a simulated call goes through the same
 * UseCase preconditions and the same state machine as a real one, so what the debug build proves about
 * the UI transfers to release.
 *
 * The simulator is opt-in, off by default. Set `call.simulation.enabled=true` in `gradle.properties`
 * (or pass `-Pcall.simulation.enabled=true`) and reinstall; without it, a debug build dials for real
 * against the configured Supabase project exactly as a release build would.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugCallModule {

    @Provides
    @Singleton
    fun provideCallRepository(
        real: WebRtcCallRepository,
        simulated: MockCallRepository
    ): CallRepository = if (BuildConfig.CALL_SIMULATION_ENABLED) simulated else real

    /**
     * Derived from whichever repository was actually installed, never from the flag a second time.
     *
     * Reading the same singleton the screen uses is what keeps these two bindings from disagreeing: if
     * the real repository is installed, there is nothing to simulate, and the debug sheet gets the
     * no-op rather than a simulator whose state nobody is looking at.
     */
    @Provides
    @Singleton
    fun provideCallSimulationController(repository: CallRepository): CallSimulationController =
        when (repository) {
            is MockCallRepository -> repository
            else -> NoOpCallSimulationController
        }
}
