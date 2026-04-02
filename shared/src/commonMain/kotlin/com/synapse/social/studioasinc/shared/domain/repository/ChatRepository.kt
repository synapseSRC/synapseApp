package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getConversations(): Result<List<Conversation>>
    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun sendMessage(chatId: String, content: String, mediaUrl: String? = null, messageType: String = "text", expiresAt: String? = null, replyToId: String? = null, senderPlaintext: String? = null): Result<Message>
    suspend fun getOrCreateChat(otherUserId: String): Result<String>

    suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit>
    suspend fun deleteConversation(chatId: String): Result<Unit>
    suspend fun markMessagesAsRead(chatId: String): Result<Unit>
    suspend fun markMessagesAsDelivered(chatId: String): Result<Unit>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun deleteMessages(messageIds: List<String>): Result<Unit>
    suspend fun deleteMessageForMe(messageId: String): Result<Unit>
    suspend fun deleteMessagesForMe(messageIds: List<String>): Result<Unit>
    suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: com.synapse.social.studioasinc.shared.domain.model.StorageProvider? = null, config: com.synapse.social.studioasinc.shared.domain.model.StorageConfig? = null, onProgress: ((Int) -> Unit)? = null): Result<String>

    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit>
    fun subscribeToMessages(chatId: String): Flow<Message>
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
    suspend fun toggleMessageReaction(messageId: String, emoji: String): Result<Unit>
    suspend fun getReactionsForMessage(messageId: String): Result<List<MessageReaction>>
    suspend fun getReactionsForMessages(messages: List<Message>): List<Message>
    fun subscribeToMessageReactions(): Flow<MessageReaction>

}
