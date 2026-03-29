package com.synapse.social.studioasinc.shared.data.dto.chat

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @EncodeDefault @SerialName("is_deleted") val isDeleted: Boolean = false,
    @EncodeDefault @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("read_by") val readBy: List<String>? = null,
    @EncodeDefault @SerialName("is_encrypted") val isEncrypted: Boolean = false,
    @SerialName("encrypted_content") val encryptedContent: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val encryptionFailureReason: String? = null
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
    @EncodeDefault @SerialName("is_encrypted") val isEncrypted: Boolean = false,
    @SerialName("encrypted_content") val encryptedContent: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("delivery_status") val deliveryStatus: String = "sent"
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
    @SerialName("updated_at") val updatedAt: String? = null
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
