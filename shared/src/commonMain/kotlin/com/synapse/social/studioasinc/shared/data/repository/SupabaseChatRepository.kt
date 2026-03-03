package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseChatRepository(
    private val dataSource: SupabaseChatDataSource = SupabaseChatDataSource(),
    private val client: SupabaseClientLib = SupabaseClient.client
) : ChatRepository {

    override suspend fun getConversations(): Result<List<Conversation>> = try {
        val conversations = dataSource.getConversations().map { (participation, user) ->
            val lastMessage = dataSource.getLastMessage(participation.chatId)
            val unreadCount = dataSource.getUnreadCount(participation.chatId, participation.lastReadAt)
            
            Conversation(
                chatId = participation.chatId,
                participantId = user?.uid ?: "",
                participantName = user?.displayName ?: user?.username ?: "Unknown",
                participantAvatar = user?.avatar,
                lastMessage = lastMessage?.content ?: "No messages yet",
                lastMessageTime = lastMessage?.createdAt,
                unreadCount = unreadCount,
                isOnline = user?.status?.name == "ONLINE"
            )
        }.sortedByDescending { it.lastMessageTime }
        
        Result.success(conversations)
    } catch (e: Exception) {
        Napier.e("Error getting conversations", e)
        Result.failure(e)
    }

    override suspend fun getMessages(chatId: String, limit: Int, before: String?): Result<List<Message>> = try {
        val messages = dataSource.getMessages(chatId, limit, before).map { it.toDomain() }
        Result.success(messages)
    } catch (e: Exception) {
        Napier.e("Error getting messages", e)
        Result.failure(e)
    }

    override suspend fun sendMessage(chatId: String, content: String, mediaUrl: String?, messageType: String): Result<Message> = try {
        val message = dataSource.sendMessage(chatId, content, mediaUrl, messageType)
        Result.success(message.toDomain())
    } catch (e: Exception) {
        Napier.e("Error sending message", e)
        Result.failure(e)
    }

    override suspend fun getOrCreateChat(otherUserId: String): Result<String> = try {
        val chatId = dataSource.getOrCreateChat(otherUserId) ?: throw Exception("Failed to create chat")
        Result.success(chatId)
    } catch (e: Exception) {
        Napier.e("Error creating chat", e)
        Result.failure(e)
    }

    override suspend fun markMessagesAsRead(chatId: String): Result<Unit> = try {
        dataSource.markMessagesAsRead(chatId)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error marking messages as read", e)
        Result.failure(e)
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = try {
        dataSource.editMessage(messageId, newContent)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error editing message", e)
        Result.failure(e)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = try {
        dataSource.deleteMessage(messageId)
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error deleting message", e)
        Result.failure(e)
    }

    override suspend fun uploadMedia(chatId: String, fileBytes: ByteArray, fileName: String, contentType: String): Result<String> = try {
        val url = dataSource.uploadMedia(chatId, fileBytes, fileName, contentType)
        Result.success(url)
    } catch (e: Exception) {
        Napier.e("Error uploading media", e)
        Result.failure(e)
    }

    override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> = try {
        // Typing status - simplified for now
        Result.success(Unit)
    } catch (e: Exception) {
        Napier.e("Error broadcasting typing status", e)
        Result.failure(e)
    }

    override fun subscribeToMessages(chatId: String): Flow<Message> =
        dataSource.subscribeToMessages(chatId).map { it.toDomain() }

    override fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message> =
        dataSource.subscribeToInboxUpdates(chatIds).map { it.toDomain() }

    override fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus> =
        dataSource.subscribeToTypingStatus(chatId).map { data ->
            TypingStatus(
                userId = data["user_id"] as? String ?: "",
                chatId = chatId,
                isTyping = data["is_typing"] as? Boolean ?: false
            )
        }

    override fun subscribeToReadReceipts(chatId: String): Flow<Message> =
        dataSource.subscribeToReadReceipts(chatId).map { it.toDomain() }

    override fun getCurrentUserId(): String? = dataSource.getCurrentUserId()
}
