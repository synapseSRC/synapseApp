package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete

import androidx.compose.ui.platform.LocalLayoutDirection
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxShimmer
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.ui.inbox.theme.InboxTheme
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    conversations: List<Conversation>,
    isLoading: Boolean,
    error: String?,
    onConversationClick: (String, String, String?, String?) -> Unit,
    onRetry: () -> Unit,
    isLocked: (String) -> Boolean = { false },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    chatListLayout: ChatListLayout = ChatListLayout.DOUBLE_LINE,
    chatSwipeGesture: ChatSwipeGesture = ChatSwipeGesture.ARCHIVE,
    folders: List<ChatFolder> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onArchive: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onUndoArchive: (String) -> Unit = {},
    onAssignToFolder: (chatId: String, folderId: String) -> Unit = { _, _ -> }
) {
    when {
        isLoading && conversations.isEmpty() -> {
            InboxShimmer(
                modifier = modifier.fillMaxSize().padding(contentPadding)
            )
        }
        error != null && conversations.isEmpty() -> {
            InboxEmptyState(
                type = EmptyStateType.ERROR,
                message = error,
                onActionClick = onRetry,
                modifier = modifier.fillMaxSize().padding(contentPadding)
            )
        }
        conversations.isEmpty() -> {
            InboxEmptyState(
                type = EmptyStateType.CHATS,
                modifier = modifier.fillMaxSize().padding(contentPadding)
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Spacing.Small,
                    bottom = Spacing.Small,
                    start = contentPadding.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                    end = contentPadding.calculateEndPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)
                ),
                verticalArrangement = Arrangement.spacedBy(InboxTheme.dimens.GroupedItemGap)
            ) {
                itemsIndexed(conversations, key = { _, it -> "${it.chatId}_${it.participantId}" }) { index, conversation ->
                    val scope = rememberCoroutineScope()
                    val haptic = LocalHapticFeedback.current

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd) {
                                when (chatSwipeGesture) {
                                    ChatSwipeGesture.ARCHIVE -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onArchive(conversation.chatId)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Conversation archived",
                                                actionLabel = "Undo"
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                onUndoArchive(conversation.chatId)
                                            }
                                        }
                                        true
                                    }
                                    ChatSwipeGesture.DELETE -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDelete(conversation.chatId)
                                        true
                                    }
                                    else -> false
                                }
                            } else if (value == SwipeToDismissBoxValue.EndToStart) { // Delete
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelete(conversation.chatId)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    val isFirst = index == 0
                    val isLast = index == conversations.size - 1
                    val shape = when {
                        conversations.size == 1 -> InboxTheme.shapes.GroupedListSingleShape
                        isFirst -> InboxTheme.shapes.GroupedListTopShape
                        isLast -> InboxTheme.shapes.GroupedListBottomShape
                        else -> InboxTheme.shapes.GroupedListMiddleShape
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val isStartToEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                            
                            val color = if (isStartToEnd) {
                                when (chatSwipeGesture) {
                                    ChatSwipeGesture.ARCHIVE -> MaterialTheme.colorScheme.primary // Green
                                    ChatSwipeGesture.DELETE -> MaterialTheme.colorScheme.error // Red
                                    else -> Color.Transparent
                                }
                            } else if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                MaterialTheme.colorScheme.error // Red
                            } else {
                                Color.Transparent
                            }

                            val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                            
                            val icon = if (isStartToEnd) {
                                when (chatSwipeGesture) {
                                    ChatSwipeGesture.ARCHIVE -> Icons.Default.Archive
                                    ChatSwipeGesture.DELETE -> Icons.Default.Delete
                                    else -> Icons.Default.Archive
                                }
                            } else {
                                Icons.Default.Delete
                            }

                            val text = if (isStartToEnd) {
                                when (chatSwipeGesture) {
                                    ChatSwipeGesture.ARCHIVE -> "Archive"
                                    ChatSwipeGesture.DELETE -> "Delete"
                                    else -> ""
                                }
                            } else if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                "Delete"
                            } else {
                                ""
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(shape)
                                    .background(color)
                                    .padding(horizontal = Spacing.Medium),
                                contentAlignment = alignment
                            ) {
                                if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                            Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(Spacing.Small))
                                            Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(icon, contentDescription = text, tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(modifier = Modifier.width(Spacing.Small))
                                            Text(text, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        },
                        content = {
                            ConversationItem(
                                conversation = conversation,
                                isLocked = isLocked(conversation.chatId),
                                onClick = { onConversationClick(conversation.chatId, conversation.participantId, conversation.participantName, conversation.participantAvatar) },
                                layout = chatListLayout,
                                shape = shape,
                                folders = folders,
                                onAssignToFolder = onAssignToFolder
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: Conversation,
    isLocked: Boolean,
    onClick: () -> Unit,
    layout: ChatListLayout = ChatListLayout.DOUBLE_LINE,
    shape: Shape = InboxTheme.shapes.ChatItemCard,
    folders: List<ChatFolder> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onArchive: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onUndoArchive: (String) -> Unit = {},
    onAssignToFolder: (chatId: String, folderId: String) -> Unit = { _, _ -> }
) {
    var showFolderSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { if (folders.isNotEmpty()) showFolderSheet = true }
            )
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            AsyncImage(
                model = conversation.participantAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(InboxTheme.dimens.AvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentScale = ContentScale.Crop
            )
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(InboxTheme.dimens.OnlineIndicatorSize)
                        .clip(CircleShape)
                        .background(StatusOnline, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(Spacing.Medium).padding(end = Spacing.ExtraSmall),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = conversation.participantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimestamp(conversation.lastMessageTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (layout == ChatListLayout.DOUBLE_LINE) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(InboxTheme.dimens.UnreadBadgeSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small)
                    )

                    if (conversation.unreadCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(InboxTheme.dimens.UnreadBadgeSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFolderSheet) {
        ModalBottomSheet(onDismissRequest = { showFolderSheet = false }) {
            Text(
                text = stringResource(R.string.move_to_folder),
                style = MaterialTheme.typography.titleMedium,
                modifier = androidx.compose.ui.Modifier.padding(
                    start = Spacing.Medium,
                    end = Spacing.Medium,
                    bottom = Spacing.Small
                )
            )
            folders.forEach { folder ->
                val isAssigned = conversation.chatId in folder.includedChatIds
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    },
                    trailingContent = {
                        if (isAssigned) Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = androidx.compose.ui.Modifier.clickable {
                        onAssignToFolder(conversation.chatId, folder.id)
                        showFolderSheet = false
                    }
                )
            }
            Spacer(modifier = androidx.compose.ui.Modifier.height(Spacing.Large))
        }
    }
}

/**
 * Formats an ISO 8601 timestamp into a human-friendly relative string.
 */
private fun formatTimestamp(isoTimestamp: String?): String {
    if (isoTimestamp == null) return ""
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
        val hoursAgo = ChronoUnit.HOURS.between(instant, now)
        val daysAgo = ChronoUnit.DAYS.between(instant, now)

        when {
            minutesAgo < 1 -> "Just now"
            minutesAgo < 60 -> "${minutesAgo}m"
            hoursAgo < 24 -> "${hoursAgo}h"
            daysAgo < 7 -> {
                val formatter = DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        ""
    }
}
