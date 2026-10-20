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
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMore) onLoadMore()
    }

    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val initialKeysCaptured = remember { mutableStateOf(false) }
    val initialItemKeys = remember { mutableSetOf<String>() }
    val oldestInitialCreatedAt = remember { mutableStateOf<String?>(null) }
    // Tracks every item key ever composed so fadeInSpec fires exactly once per item.
    // Seeded with initialItemKeys so items present at first load never fade in.
    val seenItemKeys = remember { mutableSetOf<String>() }
    if (!initialKeysCaptured.value && chatItems.isNotEmpty()) {
        chatItems.forEach { item ->
            when (item) {
                is ChatListItem.DateDivider -> initialItemKeys.add("date_${item.label}")
                is ChatListItem.UnreadDivider -> initialItemKeys.add("unread_divider")
                is ChatListItem.MessageItem -> initialItemKeys.add(item.message.clientId)
            }
        }
        seenItemKeys.addAll(initialItemKeys)
        oldestInitialCreatedAt.value = messages.minByOrNull { it.createdAt }?.createdAt
        initialKeysCaptured.value = true
    }

    val reversedItems = remember(chatItems) { chatItems.reversed() }
    val groupPositions = remember(reversedItems) {
        val messageEntries = reversedItems.mapIndexedNotNull { index, item ->
            (item as? ChatListItem.MessageItem)?.let { index to it.message }
        }
        val positions = MutableList<GroupPosition?>(reversedItems.size) { null }

        messageEntries.forEachIndexed { messageIndex, entry ->
            val itemIndex = entry.first
            val message = entry.second
            val olderMessage = messageEntries.getOrNull(messageIndex + 1)?.second
            val newerMessage = messageEntries.getOrNull(messageIndex - 1)?.second
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
    val pendingQuoteTargetId = remember { mutableStateOf<String?>(null) }
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
                        // M3 fast effects spring: damping 1.0 (no overshoot), stiffness 3800
                        fadeInSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 3800f
                        ),
                        fadeOutSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 3800f
                        ),
                        // Critically-damped spatial spring: no overshoot, fast settle.
                        // Project design decision: damping 1.0 / stiffness 1500 to
                        // prevent visible bounce when the typing indicator pushes
                        // messages upward.
                        placementSpec = spring(
                            dampingRatio = 1.0f,
                            stiffness = 1500f
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

            // Track whether this is the very first time this item is composed.
            // Once an item is added to seenItemKeys, fadeInSpec will be null
            // for all future recompositions — preventing flicker when the
            // LazyColumn recycles items (e.g. composer height changes on typing).
            val isFirstAppearance = if (!isHistorical && itemKey !in seenItemKeys) {
                seenItemKeys.add(itemKey)
                true
            } else {
                seenItemKeys.add(itemKey) // still track it
                false
            }

            // Shared placement and entrance spec for items.
            // Project design decision: critically-damped spatial spring
            // (damping 1.0 / stiffness 1500) so existing bubbles slide into
            // their new positions without any visible bounce or overshoot.
            // Using damping 1.0 instead of M3's standard 0.9 because the
            // underdamped bounce was perceptible in a reverse-layout chat
            // list and looked glitchy rather than expressive.
            val placementModifier = if (!reducedMotion) {
                Modifier.animateItem(
                    // From M3 tokens: "fast effects" spring — damping 1.0
                    // (no overshoot), stiffness 3800 (fast settle). Effects
                    // springs are for color/opacity and must never overshoot.
                    // Only active on first appearance; null on all subsequent
                    // recompositions to prevent flicker on item recycling.
                    fadeInSpec = if (isFirstAppearance) spring(
                        dampingRatio = 1.0f,
                        stiffness = 3800f
                    ) else null,
                    fadeOutSpec = spring(
                        dampingRatio = 1.0f,
                        stiffness = 3800f
                    ),
                    placementSpec = spring(
                        dampingRatio = 1.0f,
                        stiffness = 1500f
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
