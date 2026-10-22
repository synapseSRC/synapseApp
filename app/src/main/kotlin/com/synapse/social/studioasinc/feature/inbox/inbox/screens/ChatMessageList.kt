package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.feature.inbox.inbox.components.DateDividerChip
import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.inbox.inbox.components.UnreadDividerRow
import com.synapse.social.studioasinc.feature.inbox.inbox.components.isWithinTimeThreshold
import com.synapse.social.studioasinc.feature.inbox.inbox.components.quotedSenderDisplayName
import com.synapse.social.studioasinc.feature.inbox.inbox.components.TypingIndicator
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import androidx.compose.animation.core.spring
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType

@Composable
internal fun ChatMessageList(
    chatId: String,
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
    onOpenEncryption: () -> Unit = {},
    onOpenBatchGallery: (Message) -> Unit = {},
    onVoteOption: (messageId: String, optionId: String) -> Unit = { _, _ -> },
    onOpenPost: (String) -> Unit = {},
    onOpenStory: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onOpenEphemeralMedia: (String) -> Unit = {},
    onOpenMap: (Double, Double) -> Unit = { _, _ -> },
    onContactAction: (String) -> Unit = {},
    bottomContentPadding: Dp = Sizes.WidthLarge,
    modifier: Modifier = Modifier,
    onLocateMessage: (String) -> Unit = {},
    groupMemberNames: Map<String, String> = emptyMap(),
    initialMessageId: String? = null
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
    // A short list can have its oldest item visible on the very first layout. Do not
    // fetch another page until the user explicitly starts dragging toward history.
    val hasDraggedSinceLastPage = remember(listState) { mutableStateOf(false) }
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) hasDraggedSinceLastPage.value = true
        }
    }
    LaunchedEffect(shouldLoadMore.value, isLoadingMore, hasDraggedSinceLastPage.value) {
        if (shouldLoadMore.value && !isLoadingMore && hasDraggedSinceLastPage.value) {
            // Consume at most one page request per user drag. A still-underfilled
            // viewport must not cascade through history as each page arrives.
            hasDraggedSinceLastPage.value = false
            onLoadMore()
        }
    }

    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val initialKeysCaptured = remember(chatId) { mutableStateOf(false) }
    val initialItemKeys = remember(chatId) { mutableSetOf<String>() }
    val oldestInitialCreatedAt = remember(chatId) { mutableStateOf<String?>(null) }
    if (!initialKeysCaptured.value && chatItems.isNotEmpty()) {
        chatItems.forEach { item ->
            when (item) {
                is ChatListItem.DateDivider -> initialItemKeys.add("date_${item.label}")
                is ChatListItem.UnreadDivider -> initialItemKeys.add("unread_divider")
                is ChatListItem.MessageItem -> initialItemKeys.add(item.message.clientId)
            }
        }
        oldestInitialCreatedAt.value = messages.minByOrNull { it.createdAt }?.createdAt
        initialKeysCaptured.value = true
    }

    val reversedItems = remember(chatItems) { chatItems.reversed() }
    val groupPositions = remember(reversedItems) {
        // Build lookup of all messages (including non-text) for index mapping
        val messageEntries = reversedItems.mapIndexedNotNull { index, item ->
            (item as? ChatListItem.MessageItem)?.let { index to it.message }
        }
        
        // Extract only text messages for grouping purposes (non-text messages excluded from group count)
        val textMessageEntries = messageEntries.filter { (_, message) ->
            message.messageType == MessageType.TEXT
        }
        
        val positions = MutableList<GroupPosition?>(reversedItems.size) { null }

        // Compute group positions only for text messages; non-text messages are always SINGLE
        textMessageEntries.forEachIndexed { textMessageIndex, entry ->
            val itemIndex = entry.first
            val message = entry.second
            val olderMessage = textMessageEntries.getOrNull(textMessageIndex + 1)?.second
            val newerMessage = textMessageEntries.getOrNull(textMessageIndex - 1)?.second
            val hasOlder = olderMessage != null &&
                olderMessage.senderId == message.senderId &&
                isWithinTimeThreshold(olderMessage.createdAt, message.createdAt)
            val hasNewer = newerMessage != null &&
                newerMessage.senderId == message.senderId &&
                isWithinTimeThreshold(message.createdAt, newerMessage.createdAt)

            positions[itemIndex] = when {
                !hasOlder && !hasNewer -> GroupPosition.SINGLE
                !hasOlder && hasNewer -> GroupPosition.FIRST
                hasOlder && !hasNewer -> GroupPosition.LAST
                else -> GroupPosition.MIDDLE
            }
        }
        positions
    }
    val pendingQuoteTargetId = remember(chatId) { mutableStateOf<String?>(null) }
    LaunchedEffect(initialMessageId) {
        initialMessageId?.takeIf { it.isNotBlank() }?.let { messageId ->
            pendingQuoteTargetId.value = messageId
            onLocateMessage(messageId)
        }
    }
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
        // Reserve the measured composer/lock-gesture lane so the latest messages stay unobscured.
        contentPadding = PaddingValues(
            start = Spacing.space8,
            end = Spacing.space8,
            top = Spacing.space16,
            bottom = bottomContentPadding
        ),
        reverseLayout = true
    ) {
        if (typingStatus != null && typingStatus.isTyping) {
            item(key = "typing_indicator") {
                val typingModifier = if (!reducedMotion) {
                    Modifier.animateItem(
                        // M3 fast effects spring — damping 1.0 (no overshoot).
                        // Stiffness 5000 for near-instant opacity settle (~60 ms).
                        fadeInSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 5000f
                        ),
                        fadeOutSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 5000f
                        ),
                        // Critically-damped spatial spring: stiffness 3000 for
                        // fast slide-up (~80 ms) when the indicator pushes bubbles.
                        placementSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 3000f
                        )
                    )
                } else {
                    Modifier
                }
                TypingIndicator(modifier = typingModifier.padding(bottom = Spacing.space8))
            }
        }

        itemsIndexed(reversedItems, key = { _, item ->
            when (item) {
                 is ChatListItem.DateDivider -> "date_${item.label}"
                 is ChatListItem.UnreadDivider -> "unread_divider"
                 is ChatListItem.MessageItem -> item.message.clientId
             }
         }) { index, item ->
            val itemKey = when (item) {
                is ChatListItem.DateDivider -> "date_${item.label}"
                is ChatListItem.UnreadDivider -> "unread_divider"
                is ChatListItem.MessageItem -> item.message.clientId
            }
            val oldestCreatedAt = oldestInitialCreatedAt.value
            val isHistorical = itemKey in initialItemKeys || (
                item is ChatListItem.MessageItem &&
                oldestCreatedAt != null &&
                item.message.createdAt < oldestCreatedAt
            )

            // Shared placement and exit spec for items.
            // Appear animation is removed as requested; items appear instantly.
            val placementModifier = if (!reducedMotion && !isHistorical) {
                Modifier.animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = spring(
                        dampingRatio = 1.0f,
                        stiffness = 5000f
                    ),
                    placementSpec = spring(
                        dampingRatio = 1.0f,
                        stiffness = 3000f
                    )
                )
            } else {
                Modifier
            }

            when (item) {
                is ChatListItem.DateDivider -> DateDividerChip(
                    label = item.label,
                    modifier = placementModifier
                )
                is ChatListItem.UnreadDivider -> UnreadDividerRow(
                    count = item.count,
                    modifier = placementModifier
                )
                is ChatListItem.MessageItem -> {
                    val message = item.message
                    val position = groupPositions.getOrNull(index) ?: GroupPosition.SINGLE
                    val isSelected = message.id in selectedMessageIds
                    val quotedOriginal = message.replyToId?.let(messagesMap::get)
                    LaunchedEffect(message.replyToId, quotedOriginal?.id) {
                        val targetId = message.replyToId?.takeIf { it.isNotBlank() && quotedOriginal == null }
                        targetId?.let(onLocateMessage)
                    }
                    val bottomGap = if (position == GroupPosition.LAST || position == GroupPosition.SINGLE) Spacing.space12 else Spacing.space2
                    MessageBubble(
                        modifier = placementModifier.padding(bottom = bottomGap),
                        message = message,
                        isFromMe = message.isFromMe(currentUserId),
                        position = position,
                        isSelected = isSelected,
                        reactions = message.reactions.map { it.key.emoji to it.value },
                        onClick = {
                            if (selectedMessageIds.isNotEmpty()) {
                                message.id.let { onToggleSelection(it) }
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
                                message.id.let { onToggleSelection(it) }
                            } else {
                                message.id.let { onToggleSelection(it) }
                            }
                        },
                        onReactionSelected = { reaction ->                         message.id.let { onReactionSelected(it, reaction) } },
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
                    onViewProfile = { (participantProfile?.uid ?: participantId)?.let(onNavigateToProfile) },
                    onOpenEncryption = onOpenEncryption
                )
            }
        }

        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.space8), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
