package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onReplyInThread: (Message) -> Unit = {},
    onQuoteInReply: (Message) -> Unit = {},
    onForwardMessage: (Message) -> Unit = {},
    onMarkAsUnread: (Message) -> Unit = {},
    onStarMessage: (Message) -> Unit = {},
    onPinToBoard: (Message) -> Unit = {},
    onAddToTasks: (Message) -> Unit = {},
    onForwardToInbox: (Message) -> Unit = {},
    onCopyMessageLink: (Message) -> Unit = {},
    onSendFeedback: (Message) -> Unit = {}
) {
    if (selectedMessage == null) return

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1A1A1A)
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2C))
                            .clickable {
                                val sharedReaction = SharedReactionType.fromString(reaction.name)
                                selectedMessage.id?.let { onReactionSelected(it, sharedReaction) }
                                onDismissRequest()
                            }
                    ) {
                        Text(
                            text = reaction.emoji,
                            fontSize = 26.sp,
                            modifier = Modifier.padding(Spacing.ExtraSmall)
                        )
                    }
                }
            }

            HorizontalDivider()

            val isFromMe = selectedMessage.senderId == currentUserId

            // Group 1
            if (isFromMe) {
                ContextMenuRow(
                    text = "Edit",
                    icon = Icons.Default.Edit,
                    onClick = {
                        onStartEditing(selectedMessage)
                        onDismissRequest()
                    }
                )
            }
            ContextMenuRow(
                text = "Reply in thread",
                icon = Icons.Default.Forum,
                onClick = {
                    onReplyInThread(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Quote in reply",
                icon = Icons.AutoMirrored.Filled.Reply,
                onClick = {
                    onQuoteInReply(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Forward message",
                icon = Icons.AutoMirrored.Filled.Forward,
                onClick = {
                    onForwardMessage(selectedMessage)
                    onDismissRequest()
                }
            )

            HorizontalDivider()

            // Group 2
            ContextMenuRow(
                text = "Mark as unread",
                icon = Icons.Default.MarkChatUnread,
                onClick = {
                    onMarkAsUnread(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Star",
                icon = Icons.Default.StarBorder,
                onClick = {
                    onStarMessage(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Pin to board",
                icon = Icons.Default.PushPin,
                onClick = {
                    onPinToBoard(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Add to Tasks",
                icon = Icons.Default.AddTask,
                onClick = {
                    onAddToTasks(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Forward to inbox",
                icon = Icons.Default.MoveToInbox,
                onClick = {
                    onForwardToInbox(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = stringResource(R.string.action_copy),
                icon = Icons.Default.ContentCopy,
                onClick = {
                    clipboard.setText(AnnotatedString(selectedMessage.content))
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Copy message link",
                icon = Icons.Default.Link,
                onClick = {
                    onCopyMessageLink(selectedMessage)
                    onDismissRequest()
                }
            )

            HorizontalDivider()

            // Group 3
            ContextMenuRow(
                text = "Send feedback on this message",
                icon = Icons.Default.HelpOutline,
                onClick = {
                    onSendFeedback(selectedMessage)
                    onDismissRequest()
                }
            )
            ContextMenuRow(
                text = "Delete",
                icon = Icons.Default.Delete,
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    selectedMessage.id?.let { onDeleteMessageForMe(it) }
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
fun ContextMenuRow(
    text: String,
    icon: ImageVector,
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
            contentDescription = null,
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
