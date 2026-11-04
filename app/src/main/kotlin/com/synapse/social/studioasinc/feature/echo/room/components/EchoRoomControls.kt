package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.echo.room.EchoRoomUiState
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioConnection

/**
 * The room's bottom controls.
 *
 * Which controls exist is a function of the server's role, not local flags: an audience member has
 * no microphone button to press, and only a host is offered the end action.
 */
@Composable
fun EchoRoomControls(
    state: EchoRoomUiState,
    reactionEmojis: List<String>,
    onToggleMic: () -> Unit,
    onToggleHand: () -> Unit,
    onReaction: (String) -> Unit,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = state.audio == EchoAudioConnection.CONNECTED
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            if (connected && reactionEmojis.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reactionEmojis.forEach { emoji ->
                        val description = stringResource(R.string.echo_room_react_with, emoji)
                        Surface(
                            onClick = { onReaction(emoji) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.semantics { contentDescription = description }
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.space12,
                                    vertical = Spacing.space4
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12)
            ) {
                if (state.canSpeak) {
                    val micDescription = stringResource(
                        if (state.micEnabled) R.string.echo_room_mic_mute else R.string.echo_room_mic_unmute
                    )
                    FilledTonalIconButton(
                        onClick = onToggleMic,
                        enabled = !state.busy,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (state.micEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (state.micEnabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.semantics { contentDescription = micDescription }
                    ) {
                        Icon(
                            imageVector = if (state.micEnabled) EchoIcons.micOn else EchoIcons.micOff,
                            contentDescription = null
                        )
                    }
                }

                if (state.isAudience) {
                    val handDescription = stringResource(
                        if (state.handRaised) R.string.echo_room_lower_hand else R.string.echo_room_raise_hand
                    )
                    FilledTonalIconButton(
                        onClick = onToggleHand,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (state.handRaised) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (state.handRaised) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.semantics { contentDescription = handDescription }
                    ) {
                        Icon(
                            imageVector = EchoIcons.raisedHand,
                            contentDescription = null
                        )
                    }
                }

                Text(
                    text = stringResource(
                        if (state.isHost) R.string.echo_room_end_room else R.string.echo_room_leave_room
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Spacing.space4)
                )

                Button(
                    onClick = if (state.isHost) onEnd else onLeave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(
                        horizontal = Spacing.space20,
                        vertical = Spacing.space8
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (state.isHost) R.string.echo_room_end else R.string.echo_room_leave
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
