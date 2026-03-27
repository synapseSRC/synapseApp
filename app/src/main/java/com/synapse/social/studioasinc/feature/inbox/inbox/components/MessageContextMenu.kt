package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.ReactionType as AppReactionType
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    selectedMessage: Message?,
    currentUserId: String,
    onDismissRequest: () -> Unit,
    onReactionSelected: (String, SharedReactionType) -> Unit,
    onStartEditing: (Message) -> Unit,
    onDeleteMessageForMe: (String) -> Unit,
    onDeleteMessageForEveryone: (String) -> Unit,
    onSummarizeMessage: (String) -> Unit
) {
    if (selectedMessage == null) return

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.ExtraLarge)
        ) {
            // Reactions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppReactionType.values().forEach { reaction ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val sharedReaction = SharedReactionType.fromString(reaction.name)
                            selectedMessage.id?.let { onReactionSelected(it, sharedReaction) }
                            onDismissRequest()
                        }
                    ) {
                        Text(
                            text = reaction.emoji,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(Spacing.ExtraSmall)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Options
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_copy)) },
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                modifier = Modifier.clickable {
                    clipboard.setText(AnnotatedString(selectedMessage.content))
                    onDismissRequest()
                }
            )
            val isFromMe = selectedMessage.senderId == currentUserId
            if (isFromMe) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_edit)) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onStartEditing(selectedMessage)
                        onDismissRequest()
                    }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_delete_for_me)) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                modifier = Modifier.clickable {
                    selectedMessage.id?.let { onDeleteMessageForMe(it) }
                    onDismissRequest()
                }
            )
            if (isFromMe) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_delete_for_everyone), color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        selectedMessage.id?.let { onDeleteMessageForEveryone(it) }
                        onDismissRequest()
                    }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_summarize_with_ai)) },
                leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                modifier = Modifier.clickable {
                    onSummarizeMessage(selectedMessage.content)
                    onDismissRequest()
                }
            )
        }
    }
}
