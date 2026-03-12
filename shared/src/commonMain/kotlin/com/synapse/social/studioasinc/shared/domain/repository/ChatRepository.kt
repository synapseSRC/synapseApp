package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getConversations(): Result<List<Conversation>>
    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun sendMessage(chatId: String, content: String, mediaUrl: String? = null, messageType: String = "text", expiresAt: String? = null, replyToId: String? = null): Result<Message>
    suspend fun getOrCreateChat(otherUserId: String): Result<String>
    suspend fun markMessagesAsRead(chatId: String): Result<Unit>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun deleteMessageForMe(messageId: String): Result<Unit>
    suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: com.synapse.social.studioasinc.shared.domain.model.StorageProvider? = null, config: com.synapse.social.studioasinc.shared.domain.model.StorageConfig? = null, onProgress: ((Int) -> Unit)? = null): Result<String>

    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit>
    fun subscribeToMessages(chatId: String): Flow<Message>
    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message>
    fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus>
    fun subscribeToReadReceipts(chatId: String): Flow<Message>
    suspend fun initializeE2EE(): Result<Unit>
    fun getCurrentUserId(): String?
    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null): Result<String>
    suspend fun getGroupMembers(chatId: String): Result<List<Pair<com.synapse.social.studioasinc.shared.domain.model.User, Boolean>>>
    suspend fun addGroupMember(chatId: String, userId: String): Result<Unit>
    suspend fun removeGroupMember(chatId: String, userId: String): Result<Unit>
}
