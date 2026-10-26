package com.synapse.social.studioasinc.feature.inbox.inbox.screens

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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxAnimations
import com.synapse.social.studioasinc.feature.inbox.inbox.components.DateDividerChip
import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.inbox.inbox.components.SystemHistoryChip
import com.synapse.social.studioasinc.feature.inbox.inbox.components.UnreadDividerRow
import com.synapse.social.studioasinc.feature.inbox.inbox.components.isWithinTimeThreshold
import com.synapse.social.studioasinc.feature.inbox.inbox.components.quotedSenderDisplayName
import com.synapse.social.studioasinc.feature.inbox.inbox.components.TypingIndicator
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.feature.inbox.inbox.models.MessageEntranceRequest
import androidx.compose.animation.core.Animatable
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
    typingStatusDisplayName: String? = null,
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
    initialMessageId: String? = null,
    isListVisible: Boolean = true,
    isScreenVisible: Boolean
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
    // Incoming messages render with an avatar lane on the left (AvatarSmall + space8),
    // so the typing bubble is inset to sit under the sender's text column — the same
    // visual column the next incoming message will occupy, which keeps the
    // indicator→message transition free of layout jumps.
    val typingLaneInset = Sizes.AvatarSmall + Spacing.space8
    val reversedItems = remember(chatItems) { chatItems.reversed() }
    val groupingStructure = remember(reversedItems) {
        reversedItems.map { item ->
            when (item) {
                is ChatListItem.DateDivider -> Pair("date", item.label)
                is ChatListItem.UnreadDivider -> "unread"
                is ChatListItem.SystemHistoryItem -> "system"
                is ChatListItem.MessageItem -> listOf("message", item.message.clientId, item.message.senderId, item.message.createdAt, item.message.messageType.name)
            }
        }
    }
    val groupPositions = remember(groupingStructure) {
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
    LaunchedEffect(chatId, initialMessageId) {
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
            item(key = "typing_indicator", contentType = "typing_indicator") {
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
                TypingIndicator(
                    modifier = typingModifier
                        .padding(start = typingLaneInset, bottom = Spacing.space8),
                    reducedMotion = reducedMotion,
                    displayName = typingStatusDisplayName
                )
            }
        }

        itemsIndexed(reversedItems, key = { _, item ->
            when (item) {
                 is ChatListItem.DateDivider -> "date_${item.label}"
                 is ChatListItem.UnreadDivider -> "unread_divider"
                 is ChatListItem.MessageItem -> item.message.clientId
                 is ChatListItem.SystemHistoryItem -> "system_${item.message.id}"
             }
         }, contentType = { _, item ->
             when (item) {
                 is ChatListItem.DateDivider -> "date_divider"
                 is ChatListItem.UnreadDivider -> "unread_divider"
                 is ChatListItem.MessageItem -> "message"
                 is ChatListItem.SystemHistoryItem -> "system_history"
             }
         }) { index, item ->
            when (item) {
                is ChatListItem.DateDivider -> DateDividerChip(label = item.label)
                is ChatListItem.UnreadDivider -> UnreadDividerRow(count = item.count)
                is ChatListItem.SystemHistoryItem -> SystemHistoryChip(message = item.message)
                is ChatListItem.MessageItem -> {
                    val message = item.message
                    val position = groupPositions.getOrNull(index) ?: GroupPosition.SINGLE
                    val isSelected = message.id in selectedMessageIds
                    val quotedOriginal = message.replyToId?.let(messagesMap::get)
                    LaunchedEffect(message.replyToId, quotedOriginal?.id) {
                        val targetId = message.replyToId?.takeIf { it.isNotBlank() && quotedOriginal == null }
                        targetId?.let(onLocateMessage)
                    }
                    val bottomGap = if (position == GroupPosition.LAST || position == GroupPosition.SINGLE) Spacing.space12 else Spacing.space4
                    MessageBubble(
                        modifier = messageEntranceModifier(
                            chatId = chatId,
                            clientId = message.clientId,
                            listState = listState,
                            isListVisible = isListVisible,
                            reducedMotion = reducedMotion,
                            isScreenVisible = isScreenVisible,
                            request = item.entrance
                        ).padding(bottom = bottomGap),
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

@Composable
internal fun messageEntranceModifier(
    chatId: String,
    clientId: String,
    listState: LazyListState,
    isListVisible: Boolean,
    reducedMotion: Boolean,
    isScreenVisible: Boolean,
    request: MessageEntranceRequest?
): Modifier {
    if (request == null) return Modifier

    val progress = remember(chatId, clientId, request) { Animatable(if (request.isPending) 0f else 1f) }
    val offsetPx = with(LocalDensity.current) { InboxAnimations.MessageEntranceOffset.toPx() }
    LaunchedEffect(chatId, clientId, request, listState, isListVisible, isScreenVisible, reducedMotion) {
        if (!isScreenVisible || reducedMotion || !request.isPending) {
            request.consume()
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        // Lazy prefetch can compose an item before it is actually in the viewport.
        snapshotFlow {
            isListVisible && listState.layoutInfo.visibleItemsInfo.any { it.key == clientId }
        }.first { it }
        if (request.consume()) {
            try {
                progress.animateTo(1f, InboxAnimations.MessageEntranceSpec)
            } finally {
                withContext(NonCancellable) { progress.snapTo(1f) }
            }
        } else {
            progress.snapTo(1f)
        }
    }
    return Modifier.graphicsLayer {
        val value = if (reducedMotion || !isScreenVisible) 1f else progress.value
        alpha = value
        translationY = (1f - value) * offsetPx
    }
}
