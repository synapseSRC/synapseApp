package com.synapse.social.studioasinc.data.call.webrtc

import android.content.Context
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Singleton
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * Builds and binds the video surfaces the in-call UI renders into.
 *
 * This is the only place the UI layer is allowed to touch WebRTC types, and it is deliberately tiny:
 * a renderer is a view, the engine publishes tracks, and [CallMediaRegistry] joins them by call id.
 * Nothing here knows anything about call state — a surface that has no track simply shows the dark
 * background underneath it, which is the honest "no media yet" picture.
 */
@Singleton
class CallMediaHost @Inject constructor(
    private val runtime: WebRtcRuntime,
    private val registry: CallMediaRegistry
) {

    /**
     * Creates an initialized renderer bound to the shared EGL context. [fill] decides whether the
     * frame is cropped to the box (picture-in-picture preview) or letterboxed inside it (full-screen
     * remote video).
     */
    fun newRenderer(context: Context, mirror: Boolean, fill: Boolean): SurfaceViewRenderer {
        runtime.ensureInitialized()
        return SurfaceViewRenderer(context).apply {
            setMirror(mirror)
            setScalingType(
                if (fill) RendererCommon.ScalingType.SCALE_ASPECT_FILL
                else RendererCommon.ScalingType.SCALE_ASPECT_FIT
            )
            setEnableHardwareScaler(true)
            init(runtime.eglContext, null)
        }
    }

    /** Attaches [renderer] to whatever track this call publishes; returns the detach callback. */
    fun bind(callId: String, role: CallMediaRegistry.Role, renderer: SurfaceViewRenderer): () -> Unit =
        registry.attach(callId, role, renderer)

    /** Frees the renderer's EGL resources. Safe to call twice. */
    fun release(renderer: SurfaceViewRenderer) {
        runCatching { renderer.clearImage() }
        runCatching { renderer.release() }.onFailure { Napier.w("Renderer release failed: ${it.message}") }
    }
}
