package com.synapse.social.studioasinc.shared.data.dto.chat

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageAttachmentDto(
    val url: String,
    val type: String,
    val size: Long? = null,
    val duration: Int? = null,
    @SerialName("media_group_id") val mediaGroupId: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
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
    @SerialName("content_state") val contentState: String? = "active",
    @EncodeDefault @SerialName("is_deleted") val isDeleted: Boolean = false,
    @EncodeDefault @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("read_by") val readBy: List<String>? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("failure_reason") val failureReason: String? = null,
    val encryptionFailureReason: String? = null,
    val attachments: List<MessageAttachmentDto>? = null,
    @SerialName("metadata") val metadata: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatParticipantDto(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    @EncodeDefault @SerialName("is_admin") val isAdmin: Boolean = false,
    @EncodeDefault @SerialName("is_archived") val isArchived: Boolean = false,
    @EncodeDefault @SerialName("is_pinned") val isPinned: Boolean = false,
    @EncodeDefault @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("last_read_at") val lastReadAt: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewMessageDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("delivery_status") val deliveryStatus: String = "sent",
    val attachments: List<MessageAttachmentDto>? = null,
    @SerialName("metadata") val metadata: String? = null
)

@Serializable
data class UserPublicKeyDto(
    @SerialName("user_id") val userId: String, // Note: DB is UUID but Supabase serializes as String
    @SerialName("public_key") val publicKey: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("key_version") val keyVersion: Int = 1
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @EncodeDefault @SerialName("is_group") val isGroup: Boolean = false,
    @SerialName("created_by") val createdBy: String? = null,
    @EncodeDefault @SerialName("only_admins_can_message") val onlyAdminsCanMessage: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("disappearing_mode") val disappearingMode: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewChatDto(
    val id: String? = null,
    @EncodeDefault @SerialName("is_group") val isGroup: Boolean = false,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @EncodeDefault @SerialName("only_admins_can_message") val onlyAdminsCanMessage: Boolean = false
)
