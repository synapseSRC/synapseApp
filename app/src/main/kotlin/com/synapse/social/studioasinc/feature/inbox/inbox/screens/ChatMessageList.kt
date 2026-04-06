package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.inbox.inbox.components.DateDividerChip
import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.inbox.inbox.components.UnreadDividerRow
import com.synapse.social.studioasinc.feature.inbox.inbox.components.isWithinTimeThreshold
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType

@Composable
internal fun ChatMessageList(
    chatItems: List<ChatListItem>,
    messages: List<Message>,
    currentUserId: String,
    selectedMessageIds: Set<String>,
    chatFontScale: Float,
    chatMessageCornerRadius: Int,
    chatThemePreset: ChatThemePreset,
    chatAvatarDisabled: Boolean,
    participantProfile: com.synapse.social.studioasinc.shared.domain.model.User?,
    participantAvatarUrl: String?,
    isGroupChat: Boolean,
    listState: LazyListState,
    onToggleSelection: (String) -> Unit,
    onSwipeToReply: (Message) -> Unit,
    onLongClick: (Message) -> Unit,
    onReactionSelected: (String, SharedReactionType) -> Unit,
    onShowReactionPicker: (Message) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messagesMap = remember(messages) {
        messages.associateBy { it.id }
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize(),
        // Extra bottom padding so last messages aren't hidden behind the floating input
        contentPadding = PaddingValues(
            start = Spacing.Small,
            end = Spacing.Small,
            top = Spacing.Medium,
            bottom = Sizes.WidthLarge
        ),
        reverseLayout = true
    ) {
        val reversedItems = chatItems.reversed()
        itemsIndexed(reversedItems, key = { index, item ->
            when (item) {
                 is ChatListItem.DateDivider -> "date_${item.label}_$index"
                 is ChatListItem.UnreadDivider -> "unread_$index"
                 is ChatListItem.MessageItem -> item.message.id ?: "msg_$index" // Provide a fallback if id is null
             }
         }) { index, item ->
            when (item) {
                is ChatListItem.DateDivider -> DateDividerChip(label = item.label)
                is ChatListItem.UnreadDivider -> UnreadDividerRow(count = item.count)
                is ChatListItem.MessageItem -> {
                    val message = item.message
                    val prevMessageItem = reversedItems.drop(index + 1).filterIsInstance<ChatListItem.MessageItem>().firstOrNull()
                    val nextMessageItem = reversedItems.take(index).filterIsInstance<ChatListItem.MessageItem>().lastOrNull()
                    val hasOlder = prevMessageItem != null && prevMessageItem.message.senderId == message.senderId && isWithinTimeThreshold(prevMessageItem.message.createdAt, message.createdAt)
                    val hasNewer = nextMessageItem != null && nextMessageItem.message.senderId == message.senderId && isWithinTimeThreshold(message.createdAt, nextMessageItem.message.createdAt)
                    val position = when {
                        !hasOlder && !hasNewer -> GroupPosition.SINGLE
                        !hasOlder && hasNewer -> GroupPosition.FIRST
                        hasOlder && !hasNewer -> GroupPosition.LAST
                        else -> GroupPosition.MIDDLE
                    }
                    val isSelected = message.id in selectedMessageIds
                    val bottomGap = if (position == GroupPosition.LAST || position == GroupPosition.SINGLE) Spacing.Small else Spacing.Tiny
                    MessageBubble(
                        modifier = Modifier.padding(bottom = bottomGap),
                        message = message,
                        isFromMe = message.isFromMe(currentUserId),
                        position = position,
                        isSelected = isSelected,
                        onToggleSelection = { if (selectedMessageIds.isNotEmpty()) message.id?.let { onToggleSelection(it) } },
                        onSwipeToReply = { onSwipeToReply(message) },
                        replyToMessage = message.replyToId?.let { messagesMap[it] },
                        onLongClick = {
                            if (selectedMessageIds.isNotEmpty()) {
                                message.id?.let { onToggleSelection(it) }
                            } else {
                                onLongClick(message)
                            }
                        },
                        onReactionSelected = { reaction -> message.id?.let { onReactionSelected(it, reaction) } },
                        onShowReactionPicker = { onShowReactionPicker(message) },
                        fontScale = chatFontScale,
                        cornerRadius = chatMessageCornerRadius,
                        themePreset = chatThemePreset,
                        showAvatar = !chatAvatarDisabled,
                        senderName = participantProfile?.displayName ?: participantProfile?.name,
                        senderAvatarUrl = participantAvatarUrl
                    )
                }
            }
        }

        if (!isGroupChat) {
            item(key = "chat_intro_header") {
                com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatIntroHeader(
                    participantProfile = participantProfile,
                    avatarUrl = participantAvatarUrl,
                    onViewProfile = { participantProfile?.uid?.let(onNavigateToProfile) }
                )
            }
        }
    }
}
