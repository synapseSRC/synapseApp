package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.synapse.social.studioasinc.core.di.CallMediaEntryPoint
import com.synapse.social.studioasinc.data.call.webrtc.CallMediaHost
import com.synapse.social.studioasinc.data.call.webrtc.CallMediaRegistry
import dagger.hilt.android.EntryPointAccessors

/**
 * A live WebRTC video surface inside the in-call layout.
 *
 * The renderer is created once per composition, attached to the call's track through
 * [CallMediaRegistry], and released on dispose — so leaving the screen, rotating, or ending the call
 * can never leave a sink pointed at a dead native track. When the call has no track yet the surface
 * stays empty and whatever is painted behind it shows through: the UI never fakes a frame.
 */
@Composable
fun CallSurfaceVideo(
    callId: String,
    role: CallMediaRegistry.Role,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
    fill: Boolean = true
) {
    val host = rememberCallMediaHost()
    val context = LocalContext.current
    val renderer = remember(host) { host.newRenderer(context, mirror = mirror, fill = fill) }

    DisposableEffect(renderer, callId, role) {
        val detach = host.bind(callId, role, renderer)
        onDispose {
            detach()
            host.release(renderer)
        }
    }

    LaunchedEffect(renderer, mirror) {
        renderer.setMirror(mirror)
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier
    )
}

@Composable
private fun rememberCallMediaHost(): CallMediaHost {
    val context = LocalContext.current
    return remember(context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CallMediaEntryPoint::class.java
        )
        entryPoint.callMediaHost()
    }
}
