package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.data.call.webrtc.CallEngineFactory
import com.synapse.social.studioasinc.data.call.webrtc.DefaultCallEngineFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the WebRTC engine factory.
 *
 * The engines themselves are deliberately not in the graph: an engine owns a `PeerConnection`, so it
 * must be created with the call and destroyed with it — a singleton here would be a leaked microphone.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WebRtcModule {

    @Binds
    @Singleton
    abstract fun bindCallEngineFactory(factory: DefaultCallEngineFactory): CallEngineFactory
}
