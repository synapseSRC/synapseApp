package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatParticipantDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatInboxSummaryDto
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageReactionDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserPublicKeyDto
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupInvitePreview
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinOutcome
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow

class SupabaseChatDataSource(private val client: SupabaseClientLib = SupabaseClient.client) {

    private val conversations = ChatConversationDataSource(client)
    private val messages = ChatMessageDataSource(client)
    private val groups = ChatGroupDataSource(client)
    private val realtime = ChatRealtimeDataSource(client)
    private val keys = ChatKeyDataSource(client)
    private val deviceRegistration = DeviceRegistrationDataSource(client)
    private val chatSettings = ChatParticipantSettingsDataSource(client)

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getInboxSummaries(archivedOnly: Boolean = false): List<ChatInboxSummaryDto> =
        conversations.getInboxSummaries(archivedOnly)

    suspend fun getConversations(): List<Triple<ChatParticipantDto, User?, ChatDto?>> =
        conversations.getConversations()

    suspend fun getArchivedConversations(): List<Triple<ChatParticipantDto, User?, ChatDto?>> =
        conversations.getConversations(archivedOnly = true)

    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null, beforeId: String? = null): List<MessageDto> =
        messages.getMessages(chatId, limit, before, beforeId)

    suspend fun getMessagesAfter(chatId: String, after: String, afterId: String, limit: Int = 50): List<MessageDto> =
        messages.getMessagesAfter(chatId, limit, after, afterId)

    suspend fun getUserDeletedMessageIds(messageIds: List<String>): Set<String> =
        messages.getUserDeletedMessageIds(messageIds)

    suspend fun sendMessage(
        chatId: String, 
        content: String,
        mediaUrl: String? = null, 
        messageType: String = "text", 
        expiresAt: String? = null,
        replyToId: String? = null,
        attachments: List<com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto>? = null,
        metadata: String? = null,
        deviceId: String? = null
    ): MessageDto = messages.sendMessage(chatId, content, mediaUrl, messageType, expiresAt, replyToId, attachments, metadata, deviceId)

    suspend fun getOtherParticipantId(chatId: String, currentUserId: String): String? =
        keys.getOtherParticipantId(chatId, currentUserId)

    suspend fun getUserPublicKey(userId: String): UserPublicKeyDto? =
        keys.getUserPublicKey(userId)

    suspend fun uploadUserPublicKey(publicKey: String) =
        keys.uploadUserPublicKey(publicKey)

    suspend fun getOrCreateChat(otherUserId: String): String =
        conversations.getOrCreateChat(otherUserId)

    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String? = null, options: GroupCreationOptions = GroupCreationOptions()): String? =
        conversations.createGroupChat(name, participantIds, avatarUrl, options)

    suspend fun getGroupInviteCode(chatId: String): String? = groups.getGroupInviteCode(chatId)
    suspend fun rotateGroupInviteCode(chatId: String): String = groups.rotateGroupInviteCode(chatId)
    suspend fun getPendingGroupJoinRequests(chatId: String): List<GroupJoinRequest> = groups.getPendingGroupJoinRequests(chatId)
    suspend fun approveGroupJoinRequest(requestId: String) = groups.approveGroupJoinRequest(requestId)
    suspend fun declineGroupJoinRequest(requestId: String) = groups.declineGroupJoinRequest(requestId)
    suspend fun resolveGroupInvite(inviteCode: String): GroupInvitePreview? = groups.resolveGroupInvite(inviteCode)
    suspend fun requestGroupJoinByInvite(inviteCode: String): GroupJoinOutcome = groups.requestGroupJoinByInvite(inviteCode)

    suspend fun getChatInfo(chatId: String): ChatDto? =
        conversations.getChatInfo(chatId)

    suspend fun getParticipantIds(chatId: String): List<String> =
        conversations.getParticipantIds(chatId)

    suspend fun reportChat(chatId: String, reason: String) =
        groups.reportChat(chatId, reason)

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

    suspend fun updateConversationPinnedStatus(chatId: String, isPinned: Boolean) =
        conversations.updateConversationPinnedStatus(chatId, isPinned)

    suspend fun updateConversationMutedStatus(chatId: String, isMuted: Boolean) =
        conversations.updateConversationMutedStatus(chatId, isMuted)

    suspend fun deleteConversation(chatId: String) =
        conversations.deleteConversation(chatId)

    suspend fun setDisappearingMode(chatId: String, mode: String) =
        conversations.setDisappearingMode(chatId, mode)

    suspend fun getDisappearingMode(chatId: String): String? =
        conversations.getDisappearingMode(chatId)

    suspend fun insertSystemMessage(chatId: String, content: String): MessageDto =
        messages.insertSystemMessage(chatId, content)

    suspend fun deleteExpiredMessages(chatId: String): Int =
        messages.deleteExpiredMessages(chatId)

    suspend fun markMessagesAsRead(chatId: String, deviceId: String? = null, shareReceipt: Boolean = true) =
        messages.markMessagesAsRead(chatId, deviceId, shareReceipt)

    suspend fun markMessagesAsDelivered(chatId: String, deviceId: String? = null) =
        messages.markMessagesAsDelivered(chatId, deviceId)

    suspend fun getMessageById(messageId: String): MessageDto? =
        messages.getMessageById(messageId)

    suspend fun getMessagesByIds(chatId: String, messageIds: List<String>): List<MessageDto> =
        messages.getMessagesByIds(chatId, messageIds)

    suspend fun editMessage(
        messageId: String, 
        newContent: String
    ) = messages.editMessage(messageId, newContent)

    suspend fun editMessageMetadata(messageId: String, metadata: String?) = messages.editMessageMetadata(messageId, metadata)

    suspend fun markViewOnceConsumed(messageId: String) = messages.markViewOnceConsumed(messageId)

    // Device registration methods
    suspend fun registerDevice(deviceToken: String, deviceName: String? = null, deviceType: String? = null) =
        deviceRegistration.registerDevice(deviceToken, deviceName, deviceType)

    suspend fun updateDevicePresence(deviceId: String) =
        deviceRegistration.updateDevicePresence(deviceId)

    suspend fun getUserDevices() =
        deviceRegistration.getUserDevices()

    // Chat settings methods
    suspend fun getChatSettings(chatId: String) =
        chatSettings.getChatSettings(chatId)

    suspend fun updateChatSettings(
        chatId: String,
        isPinned: Boolean? = null,
        isMuted: Boolean? = null,
        isArchived: Boolean? = null,
        currentVersion: Int? = null
    ) =
        chatSettings.updateChatSettings(chatId, isPinned, isMuted, isArchived, currentVersion)

    suspend fun getAllUserSettings() =
        chatSettings.getAllUserSettings()

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

    suspend fun releaseTypingChannel(chatId: String) =
        realtime.releaseTypingChannel(chatId)

    fun subscribeToMessages(chatId: String, onConnected: suspend () -> Unit = {}): Flow<MessageDto> =
        realtime.subscribeToMessages(chatId, onConnected)

    fun subscribeToInboxUpdates(chatIds: List<String>): Flow<MessageDto> =
        realtime.subscribeToInboxUpdates(chatIds)

    fun subscribeToTypingStatus(chatId: String): Flow<Map<String, Any?>> =
        realtime.subscribeToTypingStatus(chatId)

    fun subscribeToReadReceipts(chatId: String): Flow<MessageDto> =
        realtime.subscribeToReadReceipts(chatId)

    fun subscribeToMessageReactions(chatId: String): Flow<MessageReactionDto> =
        realtime.subscribeToMessageReactions(chatId)
    suspend fun getConversationMuteStatus(chatId: String): Boolean =
        conversations.getConversationMuteStatus(chatId)

    suspend fun setConversationMuteStatus(chatId: String, isMuted: Boolean) =
        conversations.setConversationMuteStatus(chatId, isMuted)
}
