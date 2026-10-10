package com.synapse.social.studioasinc.feature.inbox.inbox.screens
import com.synapse.social.studioasinc.feature.inbox.inbox.components.TypingIndicator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.inbox.inbox.components.DateDividerChip
import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.inbox.inbox.components.UnreadDividerRow
import com.synapse.social.studioasinc.feature.inbox.inbox.components.isWithinTimeThreshold
import com.synapse.social.studioasinc.feature.inbox.inbox.components.quotedSenderDisplayName
import com.synapse.social.studioasinc.feature.inbox.inbox.components.TypingIndicator
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
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
    initialParticipantName: String?,
    participantAvatarUrl: String?,
    participantId: String?,
    isGroupChat: Boolean,
    listState: LazyListState,
    isLoadingMore: Boolean,
    typingStatus: TypingStatus?,
    onLoadMore: () -> Unit,
    onSingleTap: (Message) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSwipeToReply: (Message) -> Unit,
    onRetryClick: (Message) -> Unit = {},
    onReactionSelected: (String, SharedReactionType) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onOpenBatchGallery: (Message) -> Unit = {},
    onVoteOption: (messageId: String, optionId: String) -> Unit = { _, _ -> },
    onOpenPost: (String) -> Unit = {},
    onOpenStory: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onOpenEphemeralMedia: (String) -> Unit = {},
    onOpenMap: (Double, Double) -> Unit = { _, _ -> },
    onContactAction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onLocateMessage: (String) -> Unit = {},
    groupMemberNames: Map<String, String> = emptyMap()
) {
    val scope = rememberCoroutineScope()
    val messagesMap = remember(messages) {
        messages.associateBy { it.id }
    }
    val shouldLoadMore = remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total < 5) return@derivedStateOf false
            val highestVisibleIndex = info.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
            highestVisibleIndex >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMore) onLoadMore()
    }

    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val reversedItems = remember(chatItems) { chatItems.reversed() }
    val pendingQuoteTargetId = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingQuoteTargetId.value, reversedItems) {
        val targetId = pendingQuoteTargetId.value ?: return@LaunchedEffect
        val targetIndex = reversedItems.indexOfFirst {
            it is ChatListItem.MessageItem && it.message.id == targetId
        }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
            pendingQuoteTargetId.value = null
        }
    }
    fun navigateToQuote(targetId: String) {
        val targetIndex = reversedItems.indexOfFirst {
            it is ChatListItem.MessageItem && it.message.id == targetId
        }
        if (targetIndex >= 0) {
            pendingQuoteTargetId.value = null
            scope.launch { listState.animateScrollToItem(targetIndex) }
        } else {
            pendingQuoteTargetId.value = targetId
            onLocateMessage(targetId)
        }
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
        if (typingStatus != null && typingStatus.isTyping) {
            item(key = "typing_indicator") {
                TypingIndicator(modifier = Modifier.padding(bottom = Spacing.Small))
            }
        }

        itemsIndexed(reversedItems, key = { _, item ->
            when (item) {
                 is ChatListItem.DateDivider -> "date_${item.label}"
                 is ChatListItem.UnreadDivider -> "unread_divider"
                 is ChatListItem.MessageItem -> item.message.id ?: "msg_${item.message.createdAt}"
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
                    val quotedOriginal = message.replyToId?.let(messagesMap::get)
                    LaunchedEffect(message.replyToId, quotedOriginal?.id) {
                        val targetId = message.replyToId?.takeIf { it.isNotBlank() && quotedOriginal == null }
                        targetId?.let(onLocateMessage)
                    }
                    val bottomGap = if (position == GroupPosition.LAST || position == GroupPosition.SINGLE) Spacing.Small else Spacing.Tiny
                    val animateModifier = if (!reducedMotion) {
                        Modifier.animateItem(
                            fadeInSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            placementSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        )
                    } else {
                        Modifier
                    }
                    MessageBubble(
                        modifier = animateModifier.padding(bottom = bottomGap),
                        message = message,
                        isFromMe = message.isFromMe(currentUserId),
                        position = position,
                        isSelected = isSelected,
                        reactions = message.reactions.map { it.key.emoji to it.value },
                        onClick = {
                            if (selectedMessageIds.isNotEmpty()) {
                                message.id?.let { onToggleSelection(it) }
                            } else {
                                onSingleTap(message)
                            }
                        },
                        onSwipeToReply = { onSwipeToReply(message) },
                        onRetryClick = { onRetryClick(message) },
                        replyToMessage = message.replyToId?.let { messagesMap[it] },
                        replyToSenderName = quotedSenderDisplayName(
                            message = message.replyToId?.let { messagesMap[it] },
                            currentUserId = currentUserId,
                            isGroupChat = isGroupChat,
                            groupMemberNames = groupMemberNames,
                            participantDisplayName = participantProfile?.displayName
                                ?: participantProfile?.name
                                ?: participantProfile?.username,
                            initialParticipantName = initialParticipantName
                        ),
                        currentUserId = currentUserId,
                        onLongClick = {
                            if (selectedMessageIds.isNotEmpty()) {
                                message.id?.let { onToggleSelection(it) }
                            } else {
                                message.id?.let { onToggleSelection(it) }
                            }
                        },
                        onReactionSelected = { reaction -> message.id?.let { onReactionSelected(it, reaction) } },
                        onOpenBatchGallery = onOpenBatchGallery,
                        onQuoteClick = ::navigateToQuote,
                        fontScale = chatFontScale,
                        cornerRadius = chatMessageCornerRadius,
                        themePreset = chatThemePreset,
                        showAvatar = !chatAvatarDisabled,
                        senderName = participantProfile?.displayName ?: participantProfile?.name ?: initialParticipantName,
                        senderAvatarUrl = participantAvatarUrl,
                        onVoteOption = { messageId, optionId -> onVoteOption(messageId, optionId) },
                        onOpenPost = onOpenPost,
                        onOpenStory = onOpenStory,
                        onOpenEvent = onOpenEvent,
                        onOpenProduct = onOpenProduct,
                        onOpenEphemeralMedia = onOpenEphemeralMedia,
                        onOpenMap = onOpenMap,
                        onContactAction = onContactAction
                    )
                }
            }
        }

        if (!isGroupChat) {
            item(key = "chat_intro_header") {
                com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatIntroHeader(
                    participantProfile = participantProfile,
                    initialParticipantName = initialParticipantName,
                    avatarUrl = participantAvatarUrl,
                    onViewProfile = { (participantProfile?.uid ?: participantId)?.let(onNavigateToProfile) }
                )
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Small), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
