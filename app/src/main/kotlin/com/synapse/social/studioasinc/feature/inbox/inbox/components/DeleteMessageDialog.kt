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
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

/**
 * Simple confirmation dialog that asks the user to choose between deleting a message
 * for themselves only or for everyone in the conversation.
 *
 * "Delete for everyone" is only offered when [canDeleteForEveryone] is true.
 *
 * @param message The message being deleted.
 * @param canDeleteForEveryone Whether the message belongs to the current user.
 * @param onDismissRequest Called when the dialog is dismissed.
 * @param onDeleteForMe Called when the user selects "Delete for me".
 * @param onDeleteForEveryone Called when the user selects "Delete for everyone".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteMessageDialog(
    message: Message?,
    canDeleteForEveryone: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteForMe: (Message) -> Unit,
    onDeleteForEveryone: (Message) -> Unit
) {
    message?.let {
        val isDeleted = it.isDeleted || it.contentStatus == ContentStatus.DELETED
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
                    text = stringResource(R.string.delete_message_confirmation),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isDeleted || it.content.isBlank()) {
                        stringResource(R.string.chat_reply_message_deleted)
                    } else {
                        it.content
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                if (canDeleteForEveryone) {
                    TextButton(onClick = { onDeleteForEveryone(it) }) {
                        Text(
                            text = stringResource(R.string.delete_for_everyone),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    TextButton(onClick = { onDeleteForMe(it) }) {
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
                        TextButton(onClick = { onDeleteForMe(it) }) {
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
}