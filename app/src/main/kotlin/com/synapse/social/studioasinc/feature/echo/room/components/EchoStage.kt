package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomParticipant
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.ui.components.SectionHeader

/**
 * The people whose microphones are open.
 *
 * The speaking ring is driven by the transport's own active-speaker stream rather than the roster:
 * the roster says who *may* speak, and only the SFU knows who is audible right now.
 */
@Composable
fun EchoStage(
    detail: EchoRoomDetail,
    activeSpeakerIds: Set<String>,
    onParticipantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.echo_room_stage_title))
        val speakers = detail.speakers
        if (speakers.isEmpty()) {
            Text(
                text = stringResource(R.string.echo_room_stage_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space8)
            )
            return@Column
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.space8),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space12, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {
            speakers.forEach { participant ->
                EchoStageTile(
                    participant = participant,
                    speaking = activeSpeakerIds.contains(participant.userId),
                    isViewer = detail.isViewer(participant.userId),
                    manageable = detail.isHost &&
                        !detail.isViewer(participant.userId) &&
                        participant.userId != detail.room.hostId,
                    onClick = { onParticipantClick(participant.userId) }
                )
            }
        }
    }
}

@Composable
private fun EchoStageTile(
    participant: EchoRoomParticipant,
    speaking: Boolean,
    isViewer: Boolean,
    manageable: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val name = participant.displayName ?: participant.username.orEmpty()
    val label = name.ifBlank { stringResource(R.string.echo_room_member_fallback) }
    val role = when {
        isViewer -> stringResource(R.string.echo_room_you)
        participant.role == EchoRole.HOST -> stringResource(R.string.echo_room_host_fallback)
        else -> null
    }
    val description = if (speaking) {
        stringResource(R.string.echo_room_speaking, label)
    } else {
        stringResource(R.string.echo_room_manage_participant, label)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = Sizes.AvatarHuge + Spacing.space32)
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = if (speaking) Sizes.BorderSelected else Sizes.BorderHairline,
                    color = if (speaking) scheme.primary else scheme.outlineVariant,
                    shape = CircleShape
                )
                .padding(Spacing.space4)
        ) {
            CircularAvatar(
                imageUrl = participant.avatarUrl,
                contentDescription = description,
                displayName = label,
                size = Sizes.AvatarHuge,
                onClick = if (manageable) onClick else null
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.space8)
        )
        if (role != null) {
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = if (isViewer) scheme.primary else scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Everyone listening.
 *
 * A horizontal strip rather than a grid: listener counts can grow past what a stage layout is
 * designed for, and a raised hand needs to be visible without opening anything.
 */
@Composable
fun EchoAudienceRow(
    detail: EchoRoomDetail,
    onParticipantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.echo_room_audience_title))
        val audience = detail.audience
        if (audience.isEmpty()) {
            Text(
                text = stringResource(R.string.echo_room_audience_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space8)
            )
            return@Column
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.space12),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.AvatarLarge + Spacing.space16)
        ) {
            items(audience, key = { it.userId }) { participant ->
                EchoAudienceTile(
                    participant = participant,
                    isViewer = detail.isViewer(participant.userId),
                    manageable = detail.isHost && !detail.isViewer(participant.userId),
                    onClick = { onParticipantClick(participant.userId) }
                )
            }
        }
    }
}

@Composable
private fun EchoAudienceTile(
    participant: EchoRoomParticipant,
    isViewer: Boolean,
    manageable: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val name = participant.displayName ?: participant.username.orEmpty()
    val label = name.ifBlank { stringResource(R.string.echo_room_member_fallback) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            CircularAvatar(
                imageUrl = participant.avatarUrl,
                contentDescription = if (manageable) {
                    stringResource(R.string.echo_room_manage_participant, label)
                } else {
                    label
                },
                displayName = label,
                size = Sizes.AvatarLarge,
                onClick = if (manageable) onClick else null
            )
            if (participant.hasRaisedHand) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    modifier = Modifier.size(Sizes.IconDefault)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = EchoIcons.raisedHand,
                            contentDescription = stringResource(R.string.echo_room_hand_raised),
                            modifier = Modifier.size(Sizes.IconExtraSmall)
                        )
                    }
                }
            }
        }
        Text(
            text = if (isViewer) stringResource(R.string.echo_room_you) else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isViewer) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Sizes.AvatarLarge + Spacing.space8)
        )
    }
}
