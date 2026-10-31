package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.echo.messageRes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind

/** The room is still being joined; nothing is known about it yet. */
@Composable
fun EchoJoiningPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.echo_room_joining),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.space16)
        )
    }
}

/**
 * The room never opened.
 *
 * [error] is shown because the refusal is usually actionable — a block, a full stage, a dead
 * backend — and the retry re-runs the whole join, including a fresh token.
 */
@Composable
fun EchoFailedPanel(
    error: EchoErrorKind?,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoMessagePanel(
        title = stringResource(R.string.echo_room_failed_title),
        body = stringResource((error ?: EchoErrorKind.UNKNOWN).messageRes()),
        actionLabel = stringResource(R.string.echo_room_retry),
        onAction = onRetry,
        secondaryLabel = stringResource(R.string.echo_room_back_to_lobby),
        onSecondary = onClose,
        modifier = modifier
    )
}

@Composable
fun EchoEndedPanel(
    isHost: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoMessagePanel(
        title = stringResource(R.string.echo_room_ended_title),
        body = stringResource(
            if (isHost) R.string.echo_room_ended_body_host else R.string.echo_room_ended_body
        ),
        actionLabel = stringResource(R.string.echo_room_back_to_lobby),
        onAction = onClose,
        secondaryLabel = null,
        onSecondary = null,
        modifier = modifier
    )
}

@Composable
private fun EchoMessagePanel(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String?,
    onSecondary: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.space8)
        )
        TextButton(
            onClick = onAction,
            modifier = Modifier.padding(top = Spacing.space16)
        ) {
            Text(actionLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) {
                Text(secondaryLabel)
            }
        }
    }
}
