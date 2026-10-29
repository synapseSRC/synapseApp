package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.call.components.callEndReasonLabel
import com.synapse.social.studioasinc.feature.call.components.formatCallDuration
import com.synapse.social.studioasinc.feature.call.components.spokenDuration
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.call.isMissedCall
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlinx.datetime.Instant

/**
 * One ended call.
 *
 * Everything it shows is read off the session the backend recorded: a call is "missed" because it has no
 * connected timestamp and ended for a reason that means nobody answered, and a duration exists only when
 * the row has both ends of the call. Nothing here formats a phase or guesses an outcome.
 */
@Composable
fun CallRecordItem(
    session: CallSession,
    onCallClick: (CallSession) -> Unit,
    position: GroupedItemPosition = GroupedItemPosition.Single,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val connectedAt = session.connectedAtMillis
    val endedAt = session.endedAtMillis
    val durationMs = if (connectedAt != null && endedAt != null) {
        (endedAt - connectedAt).coerceAtLeast(0L)
    } else {
        null
    }
    val isMissed = isMissedCall(
        reason = session.endReason,
        connected = connectedAt != null,
        isIncoming = session.direction == CallDirection.INCOMING
    )
    val isVideo = session.mediaType == CallMediaType.VIDEO

    Surface(
        onClick = { onCallClick(session) },
        modifier = modifier.fillMaxWidth(),
        shape = position.shape,
        color = cardSurfaceColor()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = session.participant.avatarUrl,
                size = Sizes.AvatarDefault,
                displayName = session.participant.displayName
            )

            Spacer(modifier = Modifier.width(Spacing.space16))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.participant.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isMissed) scheme.error else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(Spacing.space4))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                            session.direction == CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                            else -> Icons.AutoMirrored.Filled.CallMade
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconSmall),
                        tint = if (isMissed) scheme.error else scheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(Spacing.space4))

                    val outcome = outcomeLabel(session, connected = connectedAt != null, isMissed = isMissed)
                    val durationText = durationMs?.let { formatCallDuration(it) }
                    val outcomeText = if (durationText != null) {
                        stringResource(R.string.call_summary_line, outcome, durationText)
                    } else {
                        outcome
                    }
                    val relativeText = TimestampFormatter.formatRelative(
                        Instant.fromEpochMilliseconds(endedAt ?: session.startedAtMillis).toString()
                    )
                    // The spoken form spells the duration out; the visual "1:04" reads as "one colon oh four".
                    val metaDescription = if (durationMs != null) {
                        stringResource(
                            R.string.call_summary_line,
                            stringResource(R.string.call_summary_line, outcome, spokenDuration(durationMs)),
                            relativeText
                        )
                    } else {
                        null
                    }

                    Text(
                        text = stringResource(R.string.call_summary_line, outcomeText, relativeText),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMissed) scheme.error else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (metaDescription != null) {
                            Modifier.semantics { contentDescription = metaDescription }
                        } else {
                            Modifier
                        }
                    )

                    if (isVideo) {
                        Spacer(modifier = Modifier.width(Spacing.space4))
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(Sizes.IconSmall),
                            tint = if (isMissed) scheme.error else scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.space8))

            IconButton(onClick = { onCallClick(session) }) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = stringResource(R.string.call_action_call_again),
                    tint = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun outcomeLabel(session: CallSession, connected: Boolean, isMissed: Boolean): String = when {
    connected -> stringResource(R.string.call_ended_title)
    isMissed -> stringResource(R.string.call_ended_missed)
    else -> callEndReasonLabel(session)
}
