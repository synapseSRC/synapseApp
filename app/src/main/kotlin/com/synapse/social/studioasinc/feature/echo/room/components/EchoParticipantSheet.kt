package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomParticipant
import com.synapse.social.studioasinc.ui.components.CircularAvatar

/**
 * The host's one-tap role control for a member.
 *
 * Only the direction the member can move is offered: an audience member can be moved up and a
 * speaker down, and the host themselves never reaches this sheet. The write is the host's alone —
 * the sheet asks; the server decides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoParticipantSheet(
    participant: EchoRoomParticipant,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSetRole: (EchoRole) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val name = participant.displayName ?: participant.username.orEmpty()
    val label = name.ifBlank { stringResource(R.string.echo_room_member_fallback) }
    val targetRole = if (participant.role == EchoRole.AUDIENCE) EchoRole.SPEAKER else EchoRole.AUDIENCE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.space20)
                .padding(bottom = Spacing.space24),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space16)
            ) {
                CircularAvatar(
                    imageUrl = participant.avatarUrl,
                    contentDescription = label,
                    displayName = label,
                    size = Sizes.AvatarLarge
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            if (participant.role == EchoRole.AUDIENCE) {
                                R.string.echo_room_role_listener
                            } else {
                                R.string.echo_room_role_speaker
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { onSetRole(targetRole) },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        if (targetRole == EchoRole.SPEAKER) {
                            R.string.echo_room_promote
                        } else {
                            R.string.echo_room_demote
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}
