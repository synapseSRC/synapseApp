package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun getConversations(): Result<List<Conversation>>
    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun sendMessage(chatId: String, content: String, mediaUrl: String? = null, messageType: String = "text"): Result<Message>
    suspend fun getOrCreateChat(otherUserId: String): Result<String>
    suspend fun markMessagesAsRead(chatId: String): Result<Unit>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun uploadMedia(chatId: String, fileBytes: ByteArray, fileName: String, contentType: String): Result<String>
    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit>
    fun subscribeToMessages(chatId: String): Flow<Message>
    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message>
    fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus>
    fun subscribeToReadReceipts(chatId: String): Flow<Message>
    fun getCurrentUserId(): String?
}
