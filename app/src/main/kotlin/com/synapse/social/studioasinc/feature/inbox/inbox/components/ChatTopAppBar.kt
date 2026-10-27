package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.UserAvatarWithStatus
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

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
    isMuted: Boolean = false,
    isMuteUpdating: Boolean = false,
    onClearSelection: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    onSummarizeChat: () -> Unit,
    onNavigateToGroupInfo: (String, String) -> Unit,
    onNavigateToChatInfo: (String, String) -> Unit,
    onOpenDisappearingMessages: () -> Unit,
    onStartVoiceCall: () -> Unit = {},
    onStartVideoCall: () -> Unit = {},
    showCallActions: Boolean = true,
    onNewGroup: () -> Unit = {},
    onSearchInChat: () -> Unit = {},
    onOpenMediaLinksAndDocs: () -> Unit = {},
    onToggleMuteNotifications: () -> Unit = {},
    onOpenChatTheme: () -> Unit = {},
    onReportChat: () -> Unit = {},
    onBlockChat: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onExportChat: () -> Unit = {},
    onAddShortcut: () -> Unit = {},
    onAddToFolder: () -> Unit = {}
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
                    val selectedContent = messages
                        .filter {
                            it.id in selectedMessageIds &&
                                !it.isDeleted &&
                                it.contentStatus != ContentStatus.DELETED
                        }
                        .joinToString("\n") { it.content }
                    if (selectedContent.isNotBlank()) {
                        clipboard.setText(AnnotatedString(selectedContent))
                    }
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
                        showActiveStatus = true,
                        displayName = participantProfile?.displayName ?: participantProfile?.username ?: initialParticipantName
                    )
                    Spacer(modifier = Modifier.width(Spacing.space12))
                    Column {
                        Text(
                            text = participantProfile?.displayName
                                ?: participantProfile?.username
                                ?: initialParticipantName
                                ?: participantId ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val formattedLastSeen = participantProfile?.lastSeen?.let { formatChatTimestamp(it) }?.takeIf { it.isNotBlank() }
                            val statusText = when {
                                typingStatus?.isTyping == true -> "Typing..."
                                isParticipantActive -> "Online"
                                formattedLastSeen != null -> "Last seen $formattedLastSeen"
                                else -> "Offline"
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
                            if (disappearingMode != DisappearingMode.OFF) {
                                Spacer(modifier = Modifier.width(Spacing.space8))
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Disappearing messages on",
                                    modifier = Modifier.size(Sizes.IconSmall),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (showCallActions) {
                    IconButton(onClick = onStartVoiceCall) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = stringResource(R.string.chat_action_voice_call)
                        )
                    }
                    IconButton(onClick = onStartVideoCall) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = stringResource(R.string.chat_action_video_call)
                        )
                    }
                }
                ChatOverflowMenu(
                    isMuted = isMuted,
                    isMuteUpdating = isMuteUpdating,
                    onSummarizeChat = onSummarizeChat,
                    onNewGroup = onNewGroup,
                    onViewContact = {
                        if (participantProfile?.accountType != "group") {
                            onNavigateToChatInfo(chatId, participantId ?: "")
                        } else {
                            onNavigateToGroupInfo(
                                chatId,
                                participantProfile.displayName ?: initialParticipantName ?: "Group"
                            )
                        }
                    },
                    onSearch = onSearchInChat,
                    onMediaLinksAndDocs = onOpenMediaLinksAndDocs,
                    onMuteNotifications = onToggleMuteNotifications,
                    onDisappearingMessages = onOpenDisappearingMessages,
                    onChatTheme = onOpenChatTheme,
                    onReport = onReportChat,
                    onBlock = onBlockChat,
                    onClearChat = onClearChat,
                    onExportChat = onExportChat,
                    onAddShortcut = onAddShortcut,
                    onAddToFolder = onAddToFolder
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun ChatOverflowMenu(
    isMuted: Boolean = false,
    isMuteUpdating: Boolean = false,
    onSummarizeChat: () -> Unit,
    onNewGroup: () -> Unit,
    onViewContact: () -> Unit,
    onSearch: () -> Unit,
    onMediaLinksAndDocs: () -> Unit,
    onMuteNotifications: () -> Unit,
    onDisappearingMessages: () -> Unit,
    onChatTheme: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    onAddShortcut: () -> Unit,
    onAddToFolder: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.chat_action_more_options)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                moreExpanded = false
            }
        ) {
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_summary,
                icon = Icons.Default.AutoAwesome,
                onClick = {
                    expanded = false
                    onSummarizeChat()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_new_group,
                icon = Icons.Default.GroupAdd,
                onClick = {
                    expanded = false
                    onNewGroup()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_view_contact,
                icon = Icons.Default.Person,
                onClick = {
                    expanded = false
                    onViewContact()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_search,
                icon = Icons.Default.Search,
                onClick = {
                    expanded = false
                    onSearch()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_media_links_docs,
                icon = Icons.Default.PermMedia,
                onClick = {
                    expanded = false
                    onMediaLinksAndDocs()
                }
            )
            ChatDropdownMenuItem(
                labelRes = if (isMuted) R.string.chat_menu_unmute_notifications else R.string.chat_menu_mute_notifications,
                icon = if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                enabled = !isMuteUpdating,
                onClick = {
                    expanded = false
                    onMuteNotifications()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_disappearing_messages,
                icon = Icons.Default.Timer,
                onClick = {
                    expanded = false
                    onDisappearingMessages()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_chat_theme,
                icon = Icons.Default.Palette,
                onClick = {
                    expanded = false
                    onChatTheme()
                }
            )
            Box {
                ChatDropdownMenuItem(
                    labelRes = R.string.chat_menu_more,
                    icon = Icons.Default.MoreHoriz,
                    trailingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        moreExpanded = true
                    }
                )
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false }
                ) {
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_report,
                        icon = Icons.Default.Report,
                        destructive = true,
                        onClick = {
                            moreExpanded = false
                            expanded = false
                            onReport()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_block,
                        icon = Icons.Default.Block,
                        destructive = true,
                        onClick = {
                            moreExpanded = false
                            expanded = false
                            onBlock()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_clear_chat,
                        icon = Icons.Default.Delete,
                        destructive = true,
                        onClick = {
                            moreExpanded = false
                            expanded = false
                            onClearChat()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_export_chat,
                        icon = Icons.Default.Share,
                        onClick = {
                            moreExpanded = false
                            expanded = false
                            onExportChat()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_add_shortcut,
                        icon = Icons.Default.Add,
                        onClick = {
                            moreExpanded = false
                            expanded = false
                            onAddShortcut()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_add_to_folder,
                        icon = Icons.Default.Folder,
                        onClick = {
                            moreExpanded = false
                            expanded = false
                            onAddToFolder()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatDropdownMenuItem(
    @StringRes labelRes: Int,
    icon: ImageVector,
    destructive: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(labelRes),
                color = contentColor
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
        },
        trailingIcon = trailingIcon,
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Formats an ISO or DB timestamp for "last seen" display.
 */
private fun formatChatTimestamp(isoTimestamp: String): String {
    val formatted = TimestampFormatter.formatLastSeen(isoTimestamp)
    return formatted.ifBlank {
        TimestampFormatter.formatRelative(isoTimestamp)
    }
}
