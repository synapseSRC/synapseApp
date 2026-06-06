package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.PushPin
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
import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageContextMenu(
    selectedMessage: Message?,
    currentUserId: String,
    onDismissRequest: () -> Unit,
    onReactionSelected: (String, ReactionType) -> Unit,
    onStartEditing: (Message) -> Unit,
    onDeleteMessageForMe: (String) -> Unit,
    onDeleteMessageForEveryone: (String) -> Unit,
    onSummarizeMessage: (String) -> Unit,
    onForwardMessage: () -> Unit = {},
    onMarkAsUnread: () -> Unit = {},
    onPinToBoard: () -> Unit = {}
) {
    if (selectedMessage == null) return

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
                ReactionType.values().forEach { reaction ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedMessage.id?.let { onReactionSelected(it, reaction) }
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
                icon = Icons.AutoMirrored.Filled.Forward,
                text = "Forward message",
                onClick = {
                    onForwardMessage()
                    onDismissRequest()
                }
            )

            HorizontalDivider()

            ActionRow(
                icon = Icons.Default.MarkChatUnread,
                text = "Mark as unread",
                onClick = {
                    onMarkAsUnread()
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
                icon = Icons.Default.ContentCopy,
                text = "Copy text",
                onClick = {
                    clipboard.setText(AnnotatedString(selectedMessage.content))
                    onDismissRequest()
                }
            )

            HorizontalDivider()

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
