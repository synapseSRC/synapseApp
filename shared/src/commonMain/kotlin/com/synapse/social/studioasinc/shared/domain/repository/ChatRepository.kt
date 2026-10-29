package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getConversations(forceNetwork: Boolean = false, limit: Int = 50, before: String? = null, beforeId: String? = null): Result<List<Conversation>>
    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null, beforeId: String? = null, forceNetwork: Boolean = false): Result<List<Message>>
    suspend fun getMessagesAfter(chatId: String, after: String, afterId: String, limit: Int = 50): Result<List<Message>>
    suspend fun getMessagesCacheFirst(chatId: String, limit: Int = 50, refreshInBackground: Boolean = false, before: String? = null, beforeId: String? = null): Result<List<Message>>
    suspend fun getMessageById(messageId: String): Result<Message?>
    suspend fun getMessagesByIds(chatId: String, messageIds: List<String>): Result<List<Message>>
    suspend fun searchMessages(chatId: String, query: String): Result<List<Message>>
    suspend fun sendMessage(
        chatId: String,
        content: String,
        mediaUrl: String? = null,
        messageType: String = "text",
        expiresAt: String? = null,
        replyToId: String? = null,
        senderPlaintext: String? = null,
        attachments: List<com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment>? = null,
        metadataContainer: com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer? = null
    ): Result<Message>
    suspend fun getOrCreateChat(otherUserId: String): Result<String>

    suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit>
    suspend fun getArchivedConversations(limit: Int = 50, before: String? = null, beforeId: String? = null): Result<List<Conversation>>
    suspend fun updateConversationPinnedStatus(chatId: String, isPinned: Boolean): Result<Unit>
    suspend fun updateConversationMutedStatus(chatId: String, isMuted: Boolean): Result<Unit>
    suspend fun deleteConversation(chatId: String): Result<Unit>
    /** [shareReceipt] = false reads the chat locally while telling the sender only that it arrived. */
    suspend fun markMessagesAsRead(chatId: String, shareReceipt: Boolean = true): Result<Unit>
    suspend fun markMessagesAsDelivered(chatId: String): Result<Unit>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun deleteMessages(messageIds: List<String>): Result<Unit>
    suspend fun deleteMessageForMe(messageId: String): Result<Unit>
    suspend fun deleteMessagesForMe(messageIds: List<String>): Result<Unit>
    suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: com.synapse.social.studioasinc.shared.domain.model.StorageProvider? = null, config: com.synapse.social.studioasinc.shared.domain.model.StorageConfig? = null, onProgress: ((Int) -> Unit)? = null): Result<String>

    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit>
    fun subscribeToMessages(chatId: String, onConnected: suspend () -> Unit = {}): Flow<Message>
    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message>
    fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus>
    fun subscribeToReadReceipts(chatId: String): Flow<Message>
    suspend fun initializeE2EE(): Result<Unit>
    suspend fun ensureSession(userId: String)
    fun getCurrentUserId(): String?
    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null): Result<String>
    suspend fun getParticipantIds(chatId: String): Result<List<String>>
    suspend fun getGroupMembers(chatId: String): Result<List<Pair<com.synapse.social.studioasinc.shared.domain.model.User, Boolean>>>
    suspend fun addGroupMembers(chatId: String, userIds: List<String>): Result<Unit>
    suspend fun removeGroupMember(chatId: String, userId: String): Result<Unit>
    suspend fun promoteToAdmin(chatId: String, userId: String): Result<Unit>
    suspend fun demoteAdmin(chatId: String, userId: String): Result<Unit>
    suspend fun leaveGroup(chatId: String): Result<Unit>
    suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean): Result<Unit>
    suspend fun getChatInfo(chatId: String): Result<com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo?>
    suspend fun toggleMessageReaction(messageId: String, emoji: String, chatId: String?): Result<Unit>
    suspend fun getReactionsForMessage(messageId: String): Result<List<MessageReaction>>
    suspend fun getReactionsForMessages(messages: List<Message>): List<Message>
    suspend fun clearLocalCache()
    fun subscribeToMessageReactions(chatId: String): Flow<MessageReaction>
    suspend fun setDisappearingMode(chatId: String, mode: com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode): Result<Unit>
    suspend fun getDisappearingMode(chatId: String): Result<com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode>
    suspend fun voteInPoll(messageId: String, optionId: String, pollId: String): Result<Unit>
    suspend fun markViewOnceConsumed(messageId: String): Result<Unit>

    /** Adds a system message (e.g., for disappearing mode changes) to the chat timeline. */
    suspend fun addSystemMessage(chatId: String, content: String): Result<Message>

    /** Deletes all expired messages for a chat (where expires_at < now). */
    suspend fun deleteExpiredMessages(chatId: String): Result<Int>

    /** Reads whether the authenticated participant has muted notifications for this chat. */
    suspend fun getConversationMuteStatus(chatId: String): Result<Boolean>
    /** Persists the authenticated participant's notification mute setting for this chat. */
    suspend fun setConversationMuteStatus(chatId: String, isMuted: Boolean): Result<Unit>

    /**
     * Registers the currently active chat screen by its chat ID.
     * Pass `null` when no chat screen is open (e.g. on back-navigation or screen destruction).
     * Used by the inbox refresh mechanism to avoid overwriting the active chat's real-time cache.
     */
    fun setActiveChatId(chatId: String?)

    /**
     * Returns the chat ID of the currently active chat screen, or `null` if no chat is open.
     */
    fun getActiveChatId(): String?

    /**
     * Reconciles a real-time WebSocket-delivered message into the local cache with real-time priority.
     * The message is upserted into the cache using a merge strategy that preserves optimistic pending messages.
     * Returns the message that should be displayed in the UI (may be an enriched version from cache).
     */
    suspend fun reconcileRealtimeMessage(chatId: String, message: com.synapse.social.studioasinc.shared.domain.model.chat.Message): com.synapse.social.studioasinc.shared.domain.model.chat.Message

    /**
     * Releases the cached typing broadcast channel for [chatId], if any.
     * Called when a chat screen is closed to avoid leaking WebSocket channels.
     * No-op if no channel is currently held for this chat.
     */
    suspend fun releaseTypingChannel(chatId: String)

    // Device synchronization methods
    fun setCurrentDeviceId(deviceId: String?)
    fun getCurrentDeviceId(): String?
}
