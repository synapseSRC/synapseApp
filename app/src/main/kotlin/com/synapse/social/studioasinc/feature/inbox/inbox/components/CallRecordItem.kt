package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
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
    modifier: Modifier = Modifier
) {
    val connectedAt = session.connectedAtMillis
    val endedAt = session.endedAtMillis
    val durationMs = if (connectedAt != null && endedAt != null) {
        (endedAt - connectedAt).coerceAtLeast(0L)
    } else {
        null
    }
    val isMissed = connectedAt == null &&
        session.direction == CallDirection.INCOMING &&
        session.endReason in MISSED_REASONS
    val isVideo = session.mediaType == CallMediaType.VIDEO

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCallClick(session) }
            .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = session.participant.avatarUrl,
            size = Sizes.AvatarMedium,
            displayName = session.participant.displayName
        )

        Spacer(modifier = Modifier.width(Spacing.space16))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.participant.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (isMissed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
                    tint = if (isMissed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(Spacing.space4))

                val outcome = outcomeLabel(session, connected = connectedAt != null, isMissed = isMissed)
                val durationText = durationMs?.let { formatCallDuration(it) }
                val metaText = if (durationText != null) {
                    stringResource(R.string.call_summary_line, outcome, durationText)
                } else {
                    outcome
                }
                // The spoken form spells the duration out; the visual "1:04" reads as "one colon oh four".
                val metaDescription = if (durationMs != null) {
                    stringResource(R.string.call_summary_line, outcome, spokenDuration(durationMs))
                } else {
                    null
                }

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMissed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (metaDescription != null) {
                        Modifier.semantics { contentDescription = metaDescription }
                    } else {
                        Modifier
                    }
                )

                Spacer(modifier = Modifier.width(Spacing.space8))

                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.IconSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(Spacing.space4))

                Text(
                    text = TimestampFormatter.formatRelative(
                        Instant.fromEpochMilliseconds(endedAt ?: session.startedAtMillis).toString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { onCallClick(session) }) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = stringResource(R.string.call_action_call_again),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun outcomeLabel(session: CallSession, connected: Boolean, isMissed: Boolean): String = when {
    connected -> stringResource(R.string.call_ended_title)
    isMissed -> stringResource(R.string.call_ended_missed)
    else -> callEndReasonLabel(session)
}

/** Reasons that mean the call never carried audio, so an incoming one was simply missed. */
private val MISSED_REASONS = setOf(
    CallEndReason.NO_ANSWER,
    CallEndReason.DECLINED,
    CallEndReason.CANCELLED,
    CallEndReason.UNAVAILABLE,
    CallEndReason.FAILED
)
