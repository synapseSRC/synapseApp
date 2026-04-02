package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment



import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.MaterialTheme


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

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
    onSummarizeMessage: (String) -> Unit,
    onReplyInThread: () -> Unit = {},
    onQuoteInReply: () -> Unit = {},
    onForwardMessage: () -> Unit = {},
    onMarkAsUnread: () -> Unit = {},
    onStarMessage: () -> Unit = {},
    onPinToBoard: () -> Unit = {},
    onAddToTasks: () -> Unit = {},
    onForwardToInbox: () -> Unit = {},
    onCopyMessageLink: () -> Unit = {},
    onSendFeedback: () -> Unit = {}
) {
    if (selectedMessage == null) return

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
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
                com.synapse.social.studioasinc.domain.model.ReactionType.getAllReactions().forEach { reaction ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                            .clickable {
                                val sharedReaction = SharedReactionType.fromString(reaction.name)
                                selectedMessage.id?.let { onReactionSelected(it, sharedReaction) }
                                onDismissRequest()
                            }
                    ) {
                        Text(
                            text = reaction.emoji,
                            fontSize = 26.sp
                        )
                    }
                }
            }

            HorizontalDivider()

            // Options

            val isFromMe = selectedMessage.senderId == currentUserId

            @Composable
            fun ActionRow(
                icon: ImageVector,
                text: String,
                tint: Color = MaterialTheme.colorScheme.onSurface,
                onClick: () -> Unit
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(onClick = onClick)
                        .padding(horizontal = Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Medium))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = tint
                    )
                }
            }

            // Group 1
            if (isFromMe) {
                ActionRow(
                    icon = Icons.Default.Edit,
                    text = "Edit",
                    onClick = {
                        onStartEditing(selectedMessage)
                        onDismissRequest()
                    }
                )
            }
            ActionRow(
                icon = Icons.Default.Forum,
                text = "Reply in thread",
                onClick = {
                    onReplyInThread()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.AutoMirrored.Filled.Reply,
                text = "Quote in reply",
                onClick = {
                    onQuoteInReply()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.AutoMirrored.Filled.Forward,
                text = "Forward message",
                onClick = {
                    onForwardMessage()
                    onDismissRequest()
                }
            )

            HorizontalDivider()

            // Group 2
            ActionRow(
                icon = Icons.Default.MarkChatUnread,
                text = "Mark as unread",
                onClick = {
                    onMarkAsUnread()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.StarBorder,
                text = "Star",
                onClick = {
                    onStarMessage()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.PushPin,
                text = "Pin to board",
                onClick = {
                    onPinToBoard()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.AddTask,
                text = "Add to Tasks",
                onClick = {
                    onAddToTasks()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.MoveToInbox,
                text = "Forward to inbox",
                onClick = {
                    onForwardToInbox()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.ContentCopy,
                text = "Copy text",
                onClick = {
                    clipboard.setText(AnnotatedString(selectedMessage.content))
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.Link,
                text = "Copy message link",
                onClick = {
                    onCopyMessageLink()
                    onDismissRequest()
                }
            )

            HorizontalDivider()

            // Group 3
            ActionRow(
                icon = Icons.Default.HelpOutline,
                text = "Send feedback on this message",
                onClick = {
                    onSendFeedback()
                    onDismissRequest()
                }
            )
            ActionRow(
                icon = Icons.Default.Delete,
                text = "Delete",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    selectedMessage.id?.let { onDeleteMessageForMe(it) }
                    onDismissRequest()
                }
            )
        }
    }
}
