package com.synapse.social.studioasinc.shared.data.dto.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
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
    @SerialName("read_by") val readBy: String? = null
)

@Serializable
data class ChatParticipantDto(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("last_read_at") val lastReadAt: String? = null
)

@Serializable
data class NewMessageDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null
)
