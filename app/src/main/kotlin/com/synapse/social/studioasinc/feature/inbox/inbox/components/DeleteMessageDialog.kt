package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

/**
 * M3 dialog that asks the user to choose between deleting a message for themselves only
 * or for everyone in the conversation.
 *
 * Only shown when the message belongs to the current user.
 *
 * @param message The message being deleted.
 * @param onDismissRequest Called when the dialog is dismissed.
 * @param onDeleteForMe Called when the user selects "Delete for me".
 * @param onDeleteForEveryone Called when the user selects "Delete for everyone".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteMessageDialog(
    message: Message?,
    onDismissRequest: () -> Unit,
    onDeleteForMe: (Message) -> Unit,
    onDeleteForEveryone: (Message) -> Unit
) {
    message?.let {
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.space8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    Text(
                        text = it.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    HorizontalDivider()

                    // Delete for me option
                    Text(
                        text = stringResource(R.string.delete_for_me),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.delete_for_me_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.space4))

                    // Delete for everyone option
                    Text(
                        text = stringResource(R.string.delete_for_everyone),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.delete_for_everyone_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = Spacing.space8),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    TextButton(
                        onClick = { onDeleteForEveryone(it) }
                    ) {
                        Text(
                            text = stringResource(R.string.delete_for_everyone),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onDeleteForMe(it) },
                    modifier = Modifier.padding(start = Spacing.space8)
                ) {
                    Text(
                        text = stringResource(R.string.delete_for_me),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp
        )
    }
}