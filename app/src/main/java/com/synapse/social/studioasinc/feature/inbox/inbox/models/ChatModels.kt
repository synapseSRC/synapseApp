package com.synapse.social.studioasinc.feature.inbox.inbox.models

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val content: String = "",
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("delivery_status") val deliveryStatus: String = "sent",
    @SerialName("message_state") val messageState: String = "sent",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_encrypted") val isEncrypted: Boolean = false,
    @SerialName("reactions") val reactions: String? = null,
    @SerialName("read_by") val readBy: String? = null
) {
    /** Convenience check — requires comparing against the current user ID at call site. */
    fun isFromMe(currentUserId: String): Boolean = senderId == currentUserId
}

data class Conversation(
    val chatId: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String?,
    val lastMessage: String,
    val lastMessageTime: String?,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

@Serializable
data class ChatParticipantDto(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    val role: String? = "member",
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("last_read_at") val lastReadAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DateDivider(val label: String) : ChatListItem()
    data class UnreadDivider(val count: Int) : ChatListItem()
}

@Serializable
data class NewMessageDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("delivery_status") val deliveryStatus: String = "sent",
    @SerialName("message_state") val messageState: String = "sent"
)
