package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserPublicKeyDto
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow

class SupabaseChatDataSource(private val client: SupabaseClientLib = SupabaseClient.client) {

    private val conversations = ChatConversationDataSource(client)
    private val messages = ChatMessageDataSource(client)
    private val groups = ChatGroupDataSource(client)
    private val realtime = ChatRealtimeDataSource(client)
    private val keys = ChatKeyDataSource(client)

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getConversations(): List<Triple<ChatParticipantDto, User?, ChatDto?>> =
        conversations.getConversations()

    suspend fun getLastMessage(chatId: String): MessageDto? =
        conversations.getLastMessage(chatId)

    suspend fun getUnreadCount(chatId: String, lastReadAt: String?): Int =
        conversations.getUnreadCount(chatId, lastReadAt)

    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): List<MessageDto> =
        messages.getMessages(chatId, limit, before)

    suspend fun sendMessage(
        chatId: String, 
        content: String,
        mediaUrl: String? = null, 
        messageType: String = "text", 
        isEncrypted: Boolean = false, 
        encryptedContent: String? = null,
        expiresAt: String? = null,
        replyToId: String? = null
    ): MessageDto = messages.sendMessage(chatId, content, mediaUrl, messageType, isEncrypted, encryptedContent, expiresAt, replyToId)

    suspend fun sendMessageNotification(recipientId: String, senderId: String, message: String, chatId: String) =
        messages.sendMessageNotification(recipientId, senderId, message, chatId)

    /**
     * Looks up the other participant in a chat from the chat_participants table.
     * This is more reliable than parsing chatId strings.
     */
    suspend fun getOtherParticipantId(chatId: String, currentUserId: String): String? =
        keys.getOtherParticipantId(chatId, currentUserId)

    suspend fun getUserPublicKey(userId: String): UserPublicKeyDto? =
        keys.getUserPublicKey(userId)

    suspend fun uploadUserPublicKey(publicKey: String) =
        keys.uploadUserPublicKey(publicKey)

    suspend fun getOrCreateChat(otherUserId: String): String? =
        conversations.getOrCreateChat(otherUserId)

    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null): String? =
        conversations.createGroupChat(name, participantIds, avatarUrl)

    suspend fun getChatInfo(chatId: String): ChatDto? =
        conversations.getChatInfo(chatId)

    suspend fun getParticipantIds(chatId: String): List<String> =
        conversations.getParticipantIds(chatId)

    suspend fun getGroupMembers(chatId: String): List<Pair<User, Boolean>> =
        groups.getGroupMembers(chatId)

    suspend fun addGroupMembers(chatId: String, userIds: List<String>) =
        groups.addGroupMembers(chatId, userIds)

    suspend fun removeGroupMember(chatId: String, userId: String) =
        groups.removeGroupMember(chatId, userId)

    suspend fun promoteToAdmin(chatId: String, userId: String) =
        groups.promoteToAdmin(chatId, userId)

    suspend fun demoteAdmin(chatId: String, userId: String) =
        groups.demoteAdmin(chatId, userId)

    suspend fun leaveGroup(chatId: String) =
        groups.leaveGroup(chatId)

    suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean) =
        groups.toggleOnlyAdminsCanMessage(chatId, enabled)

    suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean) =
        conversations.updateConversationArchiveStatus(chatId, isArchived)

    suspend fun deleteConversation(chatId: String) =
        conversations.deleteConversation(chatId)

    suspend fun markMessagesAsRead(chatId: String) =
        messages.markMessagesAsRead(chatId)

    suspend fun markMessagesAsDelivered(chatId: String) =
        messages.markMessagesAsDelivered(chatId)

    suspend fun getMessageById(messageId: String): MessageDto? =
        messages.getMessageById(messageId)

    suspend fun editMessage(
        messageId: String, 
        newContent: String,
        isEncrypted: Boolean = false, 
        encryptedContent: String? = null
    ) = messages.editMessage(messageId, newContent, isEncrypted, encryptedContent)

    suspend fun deleteMessage(messageId: String): Unit = 
        messages.deleteMessage(messageId)

    suspend fun deleteMessages(messageIds: List<String>): Unit =
        messages.deleteMessages(messageIds)

    suspend fun deleteMessageForMe(messageId: String): Unit =
        messages.deleteMessageForMe(messageId)

    suspend fun deleteMessagesForMe(messageIds: List<String>): Unit =
        messages.deleteMessagesForMe(messageIds)

    suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) = 
        realtime.broadcastTypingStatus(chatId, isTyping)

    fun subscribeToMessages(chatId: String): Flow<MessageDto> =
        realtime.subscribeToMessages(chatId)

    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> =
        realtime.subscribeToInboxUpdates(chatIds)

    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> =
        realtime.subscribeToTypingStatus(chatId)

    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> =
        realtime.subscribeToReadReceipts(chatId)
}
