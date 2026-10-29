package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Confirmation dialog for bulk deletion of selected messages from the top app bar.
 *
 * Asks the user to choose between deleting the selected messages for themselves only
 * or for everyone in the conversation.
 *
 * @param messageCount The number of messages selected for deletion.
 * @param canDeleteForEveryone Whether every selected message can be deleted for all participants.
 * @param onDismissRequest Called when the dialog is dismissed.
 * @param onDeleteForMe Called when the user selects "Delete for me".
 * @param onDeleteForEveryone Called when the user selects "Delete for everyone".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteSelectedMessagesDialog(
    messageCount: Int,
    canDeleteForEveryone: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.warning_icon),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.delete_messages_confirmation, messageCount),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (canDeleteForEveryone) {
                        R.string.delete_for_everyone_explanation
                    } else {
                        R.string.delete_messages_for_me_explanation
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            if (canDeleteForEveryone) {
                TextButton(onClick = onDeleteForEveryone) {
                    Text(
                        text = stringResource(R.string.delete_for_everyone),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onDeleteForMe) {
                    Text(
                        text = stringResource(R.string.delete_for_me),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.padding(end = Spacing.space8),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canDeleteForEveryone) {
                    TextButton(onClick = onDeleteForMe) {
                        Text(
                            text = stringResource(R.string.delete_for_me),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    )
}