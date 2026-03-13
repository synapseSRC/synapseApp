package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalLayoutDirection
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxShimmer
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.ui.inbox.theme.InboxTheme
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType
import com.synapse.social.studioasinc.domain.model.ChatListLayout
import com.synapse.social.studioasinc.domain.model.ChatSwipeGesture
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
    chatSwipeGesture: ChatSwipeGesture = ChatSwipeGesture.ARCHIVE
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                android.util.Log.d("ChatsTabScreen", "Swipe executed: ${chatSwipeGesture.name} on ${conversation.participantName}")
                                true
                            } else false
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
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = Spacing.Medium)
                                    .clip(shape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chatSwipeGesture.name,
                                    color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        content = {
                            ConversationItem(
                                conversation = conversation,
                                isLocked = isLocked(conversation.chatId),
                                onClick = { onConversationClick(conversation.chatId, conversation.participantId, conversation.participantName, conversation.participantAvatar) },
                                layout = chatListLayout,
                                shape = shape
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    isLocked: Boolean,
    onClick: () -> Unit,
    layout: ChatListLayout = ChatListLayout.DOUBLE_LINE,
    shape: Shape = InboxTheme.shapes.ChatItemCard
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .clickable(onClick = onClick)
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
                                    fontSize = 11.sp,
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
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
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
