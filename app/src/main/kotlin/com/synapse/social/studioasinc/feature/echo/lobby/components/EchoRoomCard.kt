package com.synapse.social.studioasinc.feature.echo.lobby.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomSummary
import com.synapse.social.studioasinc.ui.components.CircularAvatar

/**
 * One live room as the lobby lists it.
 *
 * Counts come from the lobby RPC rather than from a subscription: the card is a snapshot, and
 * pretending it is live would be a number that is wrong the moment it is drawn.
 */
@Composable
fun EchoRoomCard(
    summary: EchoRoomSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val host = summary.host
    val hostName = host.displayName?.takeIf { it.isNotBlank() }
        ?: host.username?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.echo_room_host_fallback)
    val topic = summary.room.topic

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12)
            ) {
                CircularAvatar(
                    imageUrl = host.avatarUrl,
                    contentDescription = null,
                    size = Sizes.AvatarMedium,
                    displayName = hostName
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.room.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.echo_lobby_hosted_by, hostName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LiveBadge()
            }

            if (!topic.isNullOrBlank()) {
                Text(
                    text = topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.echo_lobby_stage_count,
                        summary.speakerCount,
                        summary.speakerCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.echo_lobby_listener_count,
                        summary.listenerCount,
                        summary.listenerCount
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (summary.followedInRoom.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = stringResource(
                                R.string.echo_lobby_following_count,
                                summary.followedInRoom.size
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = Spacing.space8,
                                vertical = Spacing.space4
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = stringResource(R.string.echo_lobby_live),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4)
        )
    }
}
