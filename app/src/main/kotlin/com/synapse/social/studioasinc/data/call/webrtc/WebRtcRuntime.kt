package com.synapse.social.studioasinc.data.call.webrtc

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aakira.napier.Napier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory

/**
 * Process-wide WebRTC runtime: one native initialization, one shared EGL context and one
 * signaling thread.
 *
 * WebRTC's Java layer enforces that a [PeerConnectionFactory] and every [org.webrtc.PeerConnection]
 * it creates are used from a single thread. Because Synapse supports exactly one active call at a
 * time, a single shared signaling thread keeps that invariant trivially true; the engine funnels
 * every native call through [onSignaling].
 */
@Singleton
class WebRtcRuntime @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val signalingExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, SIGNALING_THREAD_NAME).apply { isDaemon = true }
    }

    private val signalingDispatcher: CoroutineDispatcher = signalingExecutor.asCoroutineDispatcher()

    /** Shared EGL context used by the video decoder factory and by every renderer. */
    val eglBase: EglBase by lazy { EglBase.create() }

    val eglContext: EglBase.Context get() = eglBase.eglBaseContext

    @Volatile
    private var initialized = false

    /** Loads libjingle_peerconnection_so and initializes the native layer exactly once. */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
            initialized = true
            Napier.i("WebRTC runtime initialized (native layer loaded)")
        }
    }

    /**
     * Runs [block] on the signaling thread, suspending until it completes. All WebRTC native
     * objects must be created, mutated and destroyed here.
     */
    suspend fun <T> onSignaling(block: () -> T): T = withContext(signalingDispatcher) {
        ensureInitialized()
        block()
    }

    /** Fire-and-forget post onto the signaling thread, used by observer callbacks. */
    fun postToSignaling(block: () -> Unit) {
        try {
            signalingExecutor.execute(block)
        } catch (_: RejectedExecutionException) {
            // Runtime is shutting down; nothing left to tear down.
        }
    }

    private companion object {
        const val SIGNALING_THREAD_NAME = "SynapseWebRtcSignaling"
    }
}
