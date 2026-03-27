package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.UserAvatarWithStatus
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.User
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    selectedMessageIds: Set<String>,
    messages: List<Message>,
    participantId: String?,
    participantProfile: User?,
    initialParticipantName: String?,
    initialParticipantAvatar: String?,
    typingStatus: com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus?,
    isParticipantActive: Boolean,
    chatId: String,
    disappearingMode: DisappearingMode,
    isLocked: Boolean,
    onClearSelection: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    onSummarizeChat: () -> Unit,
    onNavigateToGroupInfo: (String, String) -> Unit,
    onNavigateToUserMoreOptions: (String) -> Unit,
    onSetDisappearingMode: (DisappearingMode) -> Unit,
    onLockChat: () -> Unit,
    onUnlockChat: () -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    if (selectedMessageIds.isNotEmpty()) {
        TopAppBar(
            title = { Text(stringResource(R.string.selected_count_title, selectedMessageIds.size)) },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                }
            },
            actions = {
                IconButton(onClick = {
                    val selectedContent = messages.filter { it.id in selectedMessageIds }.joinToString("\n") { it.content }
                    clipboard.setText(AnnotatedString(selectedContent))
                    onClearSelection()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
                IconButton(onClick = onDeleteSelectedMessages) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    } else {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Participant avatar
                    UserAvatarWithStatus(
                        userId = participantId ?: "",
                        avatarUrl = participantProfile?.avatar ?: initialParticipantAvatar,
                        size = Sizes.IconMassive,
                        showActiveStatus = true
                    )
                    Spacer(modifier = Modifier.width(Spacing.SmallMedium))
                    Column {
                        Text(
                            text = participantProfile?.displayName
                                ?: participantProfile?.username
                                ?: initialParticipantName
                                ?: participantId ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val statusText = when {
                            typingStatus?.isTyping == true -> "Typing..."
                            isParticipantActive -> "Online"
                            else -> participantProfile?.lastSeen?.let { "Last seen ${formatChatTimestamp(it)}" } ?: "Offline"
                        }
                        val statusColor = when {
                            typingStatus?.isTyping == true -> MaterialTheme.colorScheme.primary
                            isParticipantActive -> StatusOnline
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onSummarizeChat) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize Chat")
                }
                IconButton(onClick = {
                    if (participantProfile?.accountType != "group") {
                        onNavigateToUserMoreOptions(participantId ?: "")
                    } else {
                        onNavigateToGroupInfo(chatId, participantProfile?.displayName ?: initialParticipantName ?: "Group")
                    }
                }) {
                    Icon(Icons.Default.Info, contentDescription = "User/Group Info")
                }
                var showMoreMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.disappearing_messages_label, disappearingMode.name)) },
                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                        onClick = {
                            val nextMode = when (disappearingMode) {
                                DisappearingMode.OFF -> DisappearingMode.TWENTY_FOUR_HOURS
                                DisappearingMode.TWENTY_FOUR_HOURS -> DisappearingMode.SEVEN_DAYS
                                DisappearingMode.SEVEN_DAYS -> DisappearingMode.OFF
                                else -> DisappearingMode.OFF
                            }
                            onSetDisappearingMode(nextMode)
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isLocked) "Unlock Chat" else "Lock Chat") },
                        leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) },
                        onClick = {
                            if (isLocked) onUnlockChat() else onLockChat()
                            showMoreMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mute_notifications_label)) },
                        leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                        onClick = { showMoreMenu = false }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

/**
 * Formats an ISO timestamp for "last seen" display.
 */
private fun formatChatTimestamp(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}
