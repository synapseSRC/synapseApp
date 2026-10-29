package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.call.webrtc.CallMediaRegistry
import com.synapse.social.studioasinc.feature.call.CallActions
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

@Composable
fun CallVideoContent(
    session: CallSession,
    elapsedMs: Long,
    actions: CallActions,
    debugToolsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val content = callMediaTextColor()
    val isIncomingRinging = session.direction == CallDirection.INCOMING &&
        session.phase == CallPhase.RINGING
    val isAwaitingConnection = session.phase == CallPhase.CONNECTING ||
        session.phase == CallPhase.RECONNECTING

    Box(modifier = modifier.fillMaxSize()) {
        RemoteCallMedia(session = session, isIncomingRinging = isIncomingRinging)

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(CallDimens.TopScrimHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.scrim.copy(alpha = CallDimens.TopScrimAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(CallDimens.BottomScrimHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            scheme.scrim.copy(alpha = CallDimens.BottomScrimAlpha)
                        )
                    )
                )
        )

        when {
            isIncomingRinging -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallAvatar(session = session, size = CallDimens.PortraitCompact)
                    Spacer(Modifier.height(Spacing.space16))
                    Text(
                        text = stringResource(R.string.call_incoming_video),
                        style = MaterialTheme.typography.bodyLarge,
                        color = content
                    )
                }
            }

            isAwaitingConnection -> CallProgressPill(
                text = callPhaseLabel(session),
                modifier = Modifier.align(Alignment.Center)
            )

            !session.isRemoteVideoEnabled -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallAvatar(session = session, size = CallDimens.Portrait)
                    Spacer(Modifier.height(Spacing.space16))
                    CallChipView(
                        chip = CallChip(
                            icon = CallIcons.videoOff,
                            label = stringResource(R.string.call_state_camera_off),
                            alarming = false
                        ),
                        overVideo = true
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.space16, vertical = Spacing.space16)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = session.participant.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isIncomingRinging) {
                        Text(
                            text = stringResource(R.string.chat_action_video_call),
                            style = MaterialTheme.typography.bodyMedium,
                            color = content.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    } else {
                        CallStatusText(
                            session = session,
                            elapsedMs = elapsedMs,
                            style = MaterialTheme.typography.bodyMedium,
                            color = content,
                            debugToolsEnabled = debugToolsEnabled,
                            onOpenDebug = actions.onOpenDebugSheet
                        )
                    }
                }
                if (debugToolsEnabled && !isIncomingRinging) {
                    CallSwitchChip(
                        icon = CallIcons.acceptCall,
                        label = stringResource(R.string.call_control_switch_to_voice),
                        onClick = { actions.onSwitchMediaType(CallMediaType.VOICE) }
                    )
                }
            }

            CallStatusChips(
                session = session,
                overVideo = true,
                showReconnecting = false,
                modifier = Modifier.padding(top = Spacing.space8)
            )

            Box(Modifier.fillMaxWidth()) {
                CallLocalPreview(
                    session = session,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(Modifier.weight(1f))

            if (isIncomingRinging) {
                // Keyed to the call: a second incoming session can replace this one without the
                // screen leaving composition, and it must start with an untouched slider.
                key(session.id) {
                    SlideToAnswerBar(
                        onAccept = actions.onAccept,
                        onDecline = actions.onDecline,
                        overMedia = true
                    )
                }
            } else {
                CallControlPanel(overMedia = true) {
                    CallControlRow(modifier = Modifier.fillMaxWidth()) {
                        VideoCallControls(session = session, actions = actions)
                    }
                }
                Spacer(Modifier.height(Spacing.space24))
                EndCallButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = actions.onEnd
                )
            }
        }
    }
}

@Composable
private fun RemoteCallMedia(session: CallSession, isIncomingRinging: Boolean) {
    val scheme = MaterialTheme.colorScheme
    // The dark layer stays whether or not a frame has arrived: "no media yet" must read as an empty
    // stage, never as a blank white surface or a borrowed still image.
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.scrim)
    ) {
        if (session.isRemoteVideoEnabled) {
            CallSurfaceVideo(
                callId = session.id,
                role = CallMediaRegistry.Role.REMOTE,
                modifier = Modifier.fillMaxSize()
            )
            val mediaAlpha = remoteMediaAlpha(session, isIncomingRinging)
            if (mediaAlpha < 1f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(scheme.scrim.copy(alpha = 1f - mediaAlpha))
                )
            }
        }
    }
}

@Composable
private fun CallLocalPreview(session: CallSession, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val content = callMediaTextColor()
    val previewShape = RoundedCornerShape(Sizes.CornerLarge)
    val description = stringResource(
        if (session.isLocalVideoEnabled) R.string.call_pip_description
        else R.string.call_pip_camera_off_description
    )

    Box(
        modifier = modifier
            .size(width = CallDimens.PipWidth, height = CallDimens.PipHeight)
            .shadow(CallDimens.PipElevation, previewShape)
            .clip(previewShape)
            .background(scheme.scrim)
            .border(CallDimens.PipBorder, content.copy(alpha = CallDimens.PipBorderAlpha), previewShape)
            .semantics { contentDescription = description }
    ) {
        if (session.isLocalVideoEnabled) {
            CallSurfaceVideo(
                callId = session.id,
                role = CallMediaRegistry.Role.LOCAL,
                modifier = Modifier.fillMaxSize(),
                mirror = session.isFrontCamera
            )
        } else {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CallIcons.videoOff,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.IconLarge),
                    tint = content
                )
            }
        }
    }
}

@Composable
private fun VideoCallControls(session: CallSession, actions: CallActions) {
    val content = callMediaTextColor()

    CallControlButton(
        icon = callAudioRouteIcon(session.audioRoute),
        label = stringResource(R.string.call_control_audio),
        stateDescription = callAudioRouteLabel(session.audioRoute),
        onClick = actions.onOpenAudioSheet,
        labelColor = content
    )
    CallControlButton(
        icon = if (session.isMuted) CallIcons.micOff else CallIcons.micOn,
        label = stringResource(R.string.call_control_mute),
        selected = session.isMuted,
        stateDescription = stringResource(
            if (session.isMuted) R.string.call_state_microphone_muted
            else R.string.call_state_microphone_on
        ),
        onClick = actions.onToggleMute,
        labelColor = content
    )
    CallControlButton(
        icon = if (session.isLocalVideoEnabled) CallIcons.videoOn else CallIcons.videoOff,
        label = stringResource(R.string.call_control_camera),
        selected = session.isLocalVideoEnabled,
        stateDescription = stringResource(
            if (session.isLocalVideoEnabled) R.string.call_state_camera_on
            else R.string.call_state_camera_off
        ),
        onClick = actions.onToggleCamera,
        labelColor = content
    )
    CallControlButton(
        icon = CallIcons.flipCamera,
        label = stringResource(R.string.call_control_flip_camera),
        stateDescription = stringResource(
            if (session.isFrontCamera) R.string.call_state_front_camera
            else R.string.call_state_rear_camera
        ),
        onClick = actions.onFlipCamera,
        labelColor = content
    )
}

private fun remoteMediaAlpha(session: CallSession, isIncomingRinging: Boolean): Float = when {
    isIncomingRinging -> CallDimens.IncomingMediaDim
    session.phase == CallPhase.CONNECTING -> CallDimens.ConnectingMediaDim
    session.phase == CallPhase.RECONNECTING -> CallDimens.OutgoingMediaDim
    session.direction == CallDirection.OUTGOING && session.connectedAtMillis == null ->
        CallDimens.OutgoingMediaDim

    else -> 1f
}
