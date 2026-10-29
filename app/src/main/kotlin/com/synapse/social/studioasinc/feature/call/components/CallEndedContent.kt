package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

@Composable
fun CallEndedContent(
    session: CallSession,
    elapsedMs: Long,
    onCallAgain: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectedAt = session.connectedAtMillis
    val wasConnected = connectedAt != null
    val durationMs = if (connectedAt != null) {
        val endedAt = session.endedAtMillis ?: (connectedAt + elapsedMs)
        (endedAt - connectedAt).coerceAtLeast(0L)
    } else {
        null
    }
    val summary = stringResource(
        R.string.call_summary_line,
        stringResource(
            if (session.mediaType == CallMediaType.VIDEO) R.string.chat_action_video_call
            else R.string.chat_action_voice_call
        ),
        stringResource(
            if (session.direction == CallDirection.INCOMING) R.string.call_direction_incoming
            else R.string.call_direction_outgoing
        )
    )

    EndedScaffold(
        icon = CallIcons.endCall,
        iconContainerColor = endedIconContainer(wasConnected, session.endReason),
        iconContentColor = endedIconContent(wasConnected, session.endReason),
        participantName = session.participant.displayName,
        headline = if (wasConnected) stringResource(R.string.call_ended_title) else callEndReasonLabel(session),
        durationMs = durationMs,
        summary = summary,
        showCallAgain = wasConnected,
        onCallAgain = onCallAgain,
        onDone = onDone,
        modifier = modifier
    )
}

/** Shown when the call could not be created at all, so there is no session to describe. */
@Composable
fun CallStartFailedContent(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    EndedScaffold(
        icon = CallIcons.unavailable,
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        participantName = null,
        headline = stringResource(R.string.call_ended_failed),
        durationMs = null,
        summary = null,
        showCallAgain = false,
        onCallAgain = {},
        onDone = onDone,
        modifier = modifier
    )
}

@Composable
private fun EndedScaffold(
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    participantName: String?,
    headline: String,
    durationMs: Long?,
    summary: String?,
    showCallAgain: Boolean,
    onCallAgain: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.space24),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(CallDimens.EndedIconCircle)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(CallDimens.EndedIcon),
                tint = iconContentColor
            )
        }

        Spacer(Modifier.height(Spacing.space24))

        participantName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.space4))
        }

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        durationMs?.let {
            Spacer(Modifier.height(Spacing.space8))
            CallDurationText(elapsedMs = it, color = scheme.onSurfaceVariant)
        }

        summary?.let {
            Spacer(Modifier.height(Spacing.space4))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = CallDimens.EndedActionMaxWidth)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showCallAgain) {
                Button(
                    onClick = onCallAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.ButtonHeightM3Medium),
                    shape = CircleShape
                ) {
                    Text(stringResource(R.string.call_action_call_again))
                }
                Spacer(Modifier.height(Spacing.space12))
            }
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.ButtonHeightM3Medium),
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.onSurface)
            ) {
                Text(stringResource(R.string.call_action_done))
            }
        }

        Spacer(Modifier.height(Spacing.space24))
    }
}

@Composable
private fun CallDurationText(elapsedMs: Long, color: Color) {
    val description = spokenDuration(elapsedMs)
    Text(
        text = formatCallDuration(elapsedMs),
        style = MaterialTheme.typography.titleLarge,
        color = color,
        modifier = Modifier.semantics { contentDescription = description }
    )
}

@Composable
fun callEndReasonLabel(session: CallSession): String =
    callEndReasonLabel(session.endReason, session.direction == CallDirection.INCOMING)

/**
 * The one table that says what an ended call is called.
 *
 * The calls tab, the end-call screen and the in-chat call log all read from here, so the same
 * outcome cannot be worded three different ways.
 */
@Composable
fun callEndReasonLabel(reason: CallEndReason?, isIncoming: Boolean): String = when (reason) {
    CallEndReason.DECLINED -> stringResource(R.string.call_ended_declined)
    CallEndReason.NO_ANSWER -> if (isIncoming) {
        stringResource(R.string.call_ended_missed)
    } else {
        stringResource(R.string.call_ended_no_answer)
    }

    CallEndReason.CANCELLED -> stringResource(R.string.call_ended_cancelled)
    CallEndReason.UNAVAILABLE -> stringResource(R.string.call_ended_unavailable)
    CallEndReason.FAILED -> stringResource(R.string.call_ended_failed)
    CallEndReason.COMPLETED, CallEndReason.REMOTE_ENDED, null ->
        stringResource(R.string.call_ended_title)
}

@Composable
private fun endedIconContainer(connected: Boolean, reason: CallEndReason?): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        connected -> scheme.primaryContainer
        reason == CallEndReason.CANCELLED -> scheme.surfaceContainerHigh
        else -> scheme.errorContainer
    }
}

@Composable
private fun endedIconContent(connected: Boolean, reason: CallEndReason?): Color {
    val scheme = MaterialTheme.colorScheme
    return when {
        connected -> scheme.onPrimaryContainer
        reason == CallEndReason.CANCELLED -> scheme.onSurfaceVariant
        else -> scheme.onErrorContainer
    }
}
