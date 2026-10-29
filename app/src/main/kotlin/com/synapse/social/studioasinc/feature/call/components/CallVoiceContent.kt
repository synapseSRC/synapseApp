package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.call.CallActions
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

@Composable
fun CallVoiceContent(
    session: CallSession,
    elapsedMs: Long,
    actions: CallActions,
    debugToolsEnabled: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val portraitSize = if (isCompact) CallDimens.PortraitCompact else CallDimens.Portrait
    val isRingingForAnswer = session.direction == CallDirection.INCOMING &&
        session.phase == CallPhase.RINGING
    val atmosphereColors = rememberCallAtmosphereColors(session.participant.avatarUrl)

    Box(modifier = modifier.fillMaxSize()) {
        CallAtmosphere(colors = atmosphereColors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.space24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (isCompact) Spacing.space8 else Spacing.space24))

            CallStatusText(
                session = session,
                elapsedMs = elapsedMs,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                debugToolsEnabled = debugToolsEnabled,
                onOpenDebug = actions.onOpenDebugSheet
            )

            Spacer(Modifier.height(Spacing.space8))

            Text(
                text = session.participant.displayName,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(Spacing.space4))

            Text(
                text = stringResource(
                    R.string.call_summary_line,
                    stringResource(R.string.chat_action_voice_call),
                    callAudioRouteLabel(session.audioRoute)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CallAvatar(
                    session = session,
                    size = portraitSize,
                    showHalo = session.phase == CallPhase.DIALING || session.phase == CallPhase.RINGING,
                    haloColor = atmosphereColors.primary
                )
                CallStatusChips(
                    session = session,
                    modifier = Modifier.padding(top = Spacing.space16),
                    showReconnecting = true
                )
            }

            Spacer(Modifier.height(Spacing.space16))

            if (isRingingForAnswer) {
                // Keyed to the call: a second incoming session can replace this one without the
                // screen leaving composition, and it must start with an untouched slider.
                key(session.id) {
                    SlideToAnswerBar(
                        onAccept = actions.onAccept,
                        onDecline = actions.onDecline
                    )
                }
                Spacer(Modifier.height(Spacing.space24))
            } else {
                CallControlPanel {
                    CallControlRow {
                        VoiceCallControls(session = session, actions = actions)
                    }
                }
                Spacer(Modifier.height(if (isCompact) Spacing.space16 else Spacing.space24))
                EndCallButton(onClick = actions.onEnd)
                Spacer(Modifier.height(Spacing.space16))
            }
        }
    }
}

@Composable
private fun VoiceCallControls(session: CallSession, actions: CallActions) {
    val speakerSelected = session.audioRoute == CallAudioRoute.SPEAKER

    CallControlButton(
        icon = if (speakerSelected) CallIcons.speakerOn else CallIcons.speakerOff,
        label = stringResource(R.string.call_control_speaker),
        selected = speakerSelected,
        stateDescription = stringResource(
            if (speakerSelected) R.string.call_state_speaker_on else R.string.call_state_speaker_off
        ),
        onClick = actions.onToggleSpeaker
    )
    CallControlButton(
        icon = if (session.isMuted) CallIcons.micOff else CallIcons.micOn,
        label = stringResource(R.string.call_control_mute),
        selected = session.isMuted,
        stateDescription = stringResource(
            if (session.isMuted) R.string.call_state_microphone_muted
            else R.string.call_state_microphone_on
        ),
        onClick = actions.onToggleMute
    )
    CallControlButton(
        icon = callAudioRouteIcon(session.audioRoute),
        label = stringResource(R.string.call_control_audio),
        stateDescription = callAudioRouteLabel(session.audioRoute),
        onClick = actions.onOpenAudioSheet
    )
    CallControlButton(
        icon = CallIcons.videoOn,
        label = stringResource(R.string.call_control_switch_to_video),
        onClick = { actions.onSwitchMediaType(CallMediaType.VIDEO) }
    )
}
