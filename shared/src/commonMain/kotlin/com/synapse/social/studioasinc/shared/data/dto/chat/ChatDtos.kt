package com.synapse.social.studioasinc.shared.data.dto.chat

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Handles decoding metadata whether it arrives from Postgres as a JSONB object,
 * an escaped string, a JSON null, or null. Prevents deserialization crashes in CDC events.
 */
@OptIn(ExperimentalSerializationApi::class)
object AnyMetadataStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AnyMetadataStringSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return try {
            decoder.decodeString()
        } catch (_: Exception) {
            null
        }
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }
}

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
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    val attachments: List<MessageAttachmentDto>? = null,
    @Serializable(with = AnyMetadataStringSerializer::class)
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
    @Serializable(with = AnyMetadataStringSerializer::class)
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
    @SerialName("disappearing_mode") val disappearingMode: String? = null,
    @SerialName("allow_member_settings_edits") val allowMemberSettingsEdits: Boolean = false,
    @SerialName("require_join_approval") val requireJoinApproval: Boolean = true,
    @SerialName("allow_member_invites") val allowMemberInvites: Boolean = false,
    @SerialName("invite_code") val inviteCode: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewChatDto(
    val id: String? = null,
    @EncodeDefault @SerialName("is_group") val isGroup: Boolean = false,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @EncodeDefault @SerialName("only_admins_can_message") val onlyAdminsCanMessage: Boolean = false,
    @SerialName("description") val description: String? = null,
    @EncodeDefault @SerialName("allow_member_settings_edits") val allowMemberSettingsEdits: Boolean = false,
    @EncodeDefault @SerialName("require_join_approval") val requireJoinApproval: Boolean = true,
    @EncodeDefault @SerialName("allow_member_invites") val allowMemberInvites: Boolean = false,
    @EncodeDefault @SerialName("disappearing_mode") val disappearingMode: String = "OFF"
)

/**
 * Minimal payload for creating a group chat.
 * Used as fallback when optional group-settings columns are not present in DB.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewMinimalGroupChatDto(
    val id: String,
    @EncodeDefault @SerialName("is_group") val isGroup: Boolean = true,
    val name: String? = null,
    @SerialName("created_by") val createdBy: String
)
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewDirectChatDto(
    val id: String,
    @EncodeDefault @SerialName("is_group") val isGroup: Boolean = false,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class GroupJoinRequestDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GroupInviteCodeDto(
    @SerialName("invite_code") val inviteCode: String? = null
)

@Serializable
data class GroupInvitePreviewDto(
    @SerialName("chat_id") val chatId: String,
    val name: String,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("require_join_approval") val requireJoinApproval: Boolean = true,
    @SerialName("member_count") val memberCount: Long = 0
)


@Serializable
data class ChatInboxSummaryDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("participant_id") val participantId: String,
    @SerialName("participant_name") val participantName: String? = null,
    @SerialName("participant_avatar") val participantAvatar: String? = null,
    @SerialName("participant_status") val participantStatus: String? = null,
    @SerialName("participant_last_seen") val participantLastSeen: String? = null,
    @SerialName("is_group") val isGroup: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Long = 0
)
