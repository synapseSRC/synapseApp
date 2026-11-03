package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/** The host has put this member on stage but the microphone has not been opened yet. */
@Composable
fun EchoInvitationBanner(
    onTakeStage: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoBanner(
        title = stringResource(R.string.echo_room_invited_title),
        body = stringResource(R.string.echo_room_invited_body),
        actionLabel = stringResource(R.string.echo_room_take_stage),
        onAction = onTakeStage,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
    )
}

/** A speaker whose microphone permission is permanently refused can only be helped by Settings. */
@Composable
fun EchoMicBlockedBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoBanner(
        body = stringResource(R.string.echo_room_mic_needed),
        actionLabel = stringResource(R.string.echo_room_open_settings),
        onAction = onOpenSettings,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier
    )
}

@Composable
fun EchoConnectionBanner(
    message: String,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoBanner(
        body = message,
        actionLabel = stringResource(R.string.echo_room_connect_action),
        onAction = onReconnect,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    )
}

/** A failure the user can only acknowledge; the room stays usable. */
@Composable
fun EchoErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    EchoBanner(
        body = message,
        actionLabel = stringResource(R.string.action_dismiss),
        onAction = onDismiss,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
    )
}

@Composable
private fun EchoBanner(
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
