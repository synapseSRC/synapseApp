package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import com.synapse.social.studioasinc.feature.call.CallActions
import com.synapse.social.studioasinc.feature.call.CallScreenContent
import com.synapse.social.studioasinc.feature.call.CallUiState
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallConnectionQuality
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallParticipant
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

private val PreviewParticipant = CallParticipant(
    userId = "preview-peer",
    displayName = "Amara Okafor"
)

private fun previewSession(
    mediaType: CallMediaType = CallMediaType.VOICE,
    direction: CallDirection = CallDirection.OUTGOING,
    phase: CallPhase = CallPhase.ACTIVE,
    endReason: CallEndReason? = null,
    isMuted: Boolean = false,
    audioRoute: CallAudioRoute = CallAudioRoute.EARPIECE,
    isLocalVideoEnabled: Boolean = mediaType == CallMediaType.VIDEO,
    isRemoteVideoEnabled: Boolean = mediaType == CallMediaType.VIDEO,
    isFrontCamera: Boolean = true,
    connectionQuality: CallConnectionQuality = CallConnectionQuality.GOOD,
    isConnectedPhase: Boolean = phase == CallPhase.ACTIVE ||
        phase == CallPhase.RECONNECTING ||
        (phase == CallPhase.ENDED && endReason == CallEndReason.COMPLETED)
): CallSession = CallSession(
    id = "preview-call",
    participant = PreviewParticipant,
    chatId = "preview-chat",
    direction = direction,
    mediaType = mediaType,
    phase = phase,
    endReason = endReason,
    isMuted = isMuted,
    audioRoute = audioRoute,
    isLocalVideoEnabled = isLocalVideoEnabled,
    isRemoteVideoEnabled = isRemoteVideoEnabled,
    isFrontCamera = isFrontCamera,
    connectionQuality = connectionQuality,
    startedAtMillis = 0L,
    connectedAtMillis = if (isConnectedPhase) 0L else null,
    endedAtMillis = if (phase == CallPhase.ENDED) 0L else null
)

@Composable
private fun CallStatePreview(
    session: CallSession,
    elapsedMs: Long = 0L,
    darkTheme: Boolean = false,
    largeFont: Boolean = false,
    debugToolsEnabled: Boolean = false
) {
    SynapseTheme(darkTheme = darkTheme, dynamicColor = false) {
        if (largeFont) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                PreviewCallScreen(session, elapsedMs, debugToolsEnabled)
            }
        } else {
            PreviewCallScreen(session, elapsedMs, debugToolsEnabled)
        }
    }
}

@Composable
private fun PreviewCallScreen(
    session: CallSession,
    elapsedMs: Long,
    debugToolsEnabled: Boolean
) {
    CallScreenContent(
        state = CallUiState(session = session, elapsedMs = elapsedMs),
        actions = CallActions(),
        debugToolsEnabled = debugToolsEnabled,
        onClose = {}
    )
}

private const val PreviewWidth = 411
private const val PreviewHeight = 914

@Preview(name = "Voice · Dialing", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceDialingPreview() {
    CallStatePreview(previewSession(phase = CallPhase.DIALING))
}

@Preview(name = "Voice · Ringing", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceRingingPreview() {
    CallStatePreview(previewSession(phase = CallPhase.RINGING))
}

@Preview(name = "Voice · Connecting", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceConnectingPreview() {
    CallStatePreview(previewSession(phase = CallPhase.CONNECTING))
}

@Preview(name = "Voice · Active", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceActivePreview() {
    CallStatePreview(previewSession(), elapsedMs = 194_000L)
}

@Preview(name = "Voice · Muted on speaker", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceMutedPreview() {
    CallStatePreview(
        previewSession(
            isMuted = true,
            audioRoute = CallAudioRoute.SPEAKER,
            connectionQuality = CallConnectionQuality.FAIR
        ),
        elapsedMs = 2_405_000L
    )
}

@Preview(name = "Voice · Poor connection", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoicePoorConnectionPreview() {
    CallStatePreview(
        previewSession(connectionQuality = CallConnectionQuality.POOR),
        elapsedMs = 61_000L
    )
}

@Preview(name = "Voice · Reconnecting", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceReconnectingPreview() {
    CallStatePreview(previewSession(phase = CallPhase.RECONNECTING), elapsedMs = 320_000L)
}

@Preview(name = "Voice · Incoming", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceIncomingPreview() {
    CallStatePreview(
        previewSession(direction = CallDirection.INCOMING, phase = CallPhase.RINGING)
    )
}

@Preview(name = "Voice · Active (dark)", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceActiveDarkPreview() {
    CallStatePreview(previewSession(), elapsedMs = 194_000L, darkTheme = true)
}

@Preview(name = "Voice · Active (large font)", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VoiceActiveLargeFontPreview() {
    CallStatePreview(previewSession(), elapsedMs = 194_000L, largeFont = true)
}

@Preview(name = "Voice · Compact height", widthDp = PreviewWidth, heightDp = 580, showBackground = true)
@Composable
private fun VoiceCompactPreview() {
    CallStatePreview(previewSession(), elapsedMs = 194_000L)
}

@Preview(name = "Video · Outgoing", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoOutgoingPreview() {
    CallStatePreview(previewSession(mediaType = CallMediaType.VIDEO, phase = CallPhase.RINGING))
}

@Preview(name = "Video · Active", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoActivePreview() {
    CallStatePreview(
        previewSession(mediaType = CallMediaType.VIDEO, audioRoute = CallAudioRoute.SPEAKER),
        elapsedMs = 152_000L
    )
}

@Preview(name = "Video · Muted with camera off", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoLocalCameraOffPreview() {
    CallStatePreview(
        previewSession(
            mediaType = CallMediaType.VIDEO,
            isMuted = true,
            audioRoute = CallAudioRoute.SPEAKER,
            isLocalVideoEnabled = false
        ),
        elapsedMs = 45_000L
    )
}

@Preview(name = "Video · Rear camera", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoRearCameraPreview() {
    CallStatePreview(
        previewSession(
            mediaType = CallMediaType.VIDEO,
            audioRoute = CallAudioRoute.SPEAKER,
            isFrontCamera = false
        ),
        elapsedMs = 87_000L
    )
}

@Preview(name = "Video · Remote camera off", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoRemoteCameraOffPreview() {
    CallStatePreview(
        previewSession(
            mediaType = CallMediaType.VIDEO,
            audioRoute = CallAudioRoute.SPEAKER,
            isRemoteVideoEnabled = false
        ),
        elapsedMs = 1_204_000L
    )
}

@Preview(name = "Video · Reconnecting", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoReconnectingPreview() {
    CallStatePreview(
        previewSession(
            mediaType = CallMediaType.VIDEO,
            phase = CallPhase.RECONNECTING,
            audioRoute = CallAudioRoute.SPEAKER
        ),
        elapsedMs = 600_000L
    )
}

@Preview(name = "Video · Incoming", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoIncomingPreview() {
    CallStatePreview(
        previewSession(
            mediaType = CallMediaType.VIDEO,
            direction = CallDirection.INCOMING,
            phase = CallPhase.RINGING
        )
    )
}

@Preview(name = "Video · Active (dark)", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoActiveDarkPreview() {
    CallStatePreview(
        previewSession(mediaType = CallMediaType.VIDEO, audioRoute = CallAudioRoute.SPEAKER),
        elapsedMs = 152_000L,
        darkTheme = true
    )
}

@Preview(name = "Video · Active (debug tools)", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun VideoActiveDebugPreview() {
    CallStatePreview(
        previewSession(mediaType = CallMediaType.VIDEO, audioRoute = CallAudioRoute.SPEAKER),
        elapsedMs = 152_000L,
        debugToolsEnabled = true
    )
}

@Preview(name = "Ended · Connected", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun EndedConnectedPreview() {
    CallStatePreview(
        previewSession(
            phase = CallPhase.ENDED,
            endReason = CallEndReason.COMPLETED,
            audioRoute = CallAudioRoute.SPEAKER
        ),
        elapsedMs = 1_845_000L
    )
}

@Preview(name = "Ended · Declined", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun EndedDeclinedPreview() {
    CallStatePreview(
        previewSession(
            direction = CallDirection.INCOMING,
            phase = CallPhase.ENDED,
            endReason = CallEndReason.DECLINED
        )
    )
}

@Preview(name = "Ended · Missed", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun EndedMissedPreview() {
    CallStatePreview(
        previewSession(
            direction = CallDirection.INCOMING,
            phase = CallPhase.ENDED,
            endReason = CallEndReason.NO_ANSWER
        )
    )
}

@Preview(name = "Ended · Cancelled", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun EndedCancelledPreview() {
    CallStatePreview(
        previewSession(phase = CallPhase.ENDED, endReason = CallEndReason.CANCELLED)
    )
}

@Preview(name = "Ended · Unavailable (dark)", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun EndedUnavailableDarkPreview() {
    CallStatePreview(
        previewSession(phase = CallPhase.ENDED, endReason = CallEndReason.UNAVAILABLE),
        darkTheme = true
    )
}

@Preview(name = "Start failed", widthDp = PreviewWidth, heightDp = PreviewHeight, showBackground = true)
@Composable
private fun StartFailedPreview() {
    SynapseTheme(dynamicColor = false) {
        CallScreenContent(
            state = CallUiState(startFailed = true),
            actions = CallActions(),
            debugToolsEnabled = false,
            onClose = {}
        )
    }
}
