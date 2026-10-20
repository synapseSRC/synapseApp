package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType
import com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import kotlinx.serialization.json.Json

object ChatMapper {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Normalizes Supabase timestamps (which in Postgres CDC may have space separators
     * like "YYYY-MM-DD HH:mm:ss.SSSSSS+00" or short offsets "+00") into canonical ISO-8601 strings.
     */
    fun normalizeTimestamp(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        val trimmed = raw.trim()
        if (trimmed.length < 19) return trimmed

        val withT = if (trimmed.length > 10 && trimmed[10] == ' ') {
            trimmed.substring(0, 10) + 'T' + trimmed.substring(11)
        } else {
            trimmed
        }

        val len = withT.length
        if (len >= 3) {
            val sign = withT[len - 3]
            val d1 = withT[len - 2]
            val d2 = withT[len - 1]
            if ((sign == '+' || sign == '-') && d1.isDigit() && d2.isDigit()) {
                return if (d1 == '0' && d2 == '0') {
                    withT.substring(0, len - 3) + "Z"
                } else {
                    withT + ":00"
                }
            }
        }
        return withT
    }

    fun ChatDto.toDomain(): ChatInfo = ChatInfo(
        id = id,
        name = name,
        description = description,
        avatarUrl = avatarUrl,
        isGroup = isGroup,
        createdBy = createdBy,
        onlyAdminsCanMessage = onlyAdminsCanMessage,
        createdAt = normalizeTimestamp(createdAt),
        updatedAt = normalizeTimestamp(updatedAt),
        disappearingMode = try {
            disappearingMode?.let { com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode.valueOf(it) }
                ?: com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode.OFF
        } catch (e: Exception) {
            com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode.OFF
        }
    )

    fun MessageAttachmentDto.toDomain(): MessageAttachment = MessageAttachment(
        url = url,
        type = when (type.lowercase()) {
            "video" -> AttachmentType.VIDEO
            "audio" -> AttachmentType.AUDIO
            "file" -> AttachmentType.FILE
            else -> AttachmentType.IMAGE
        },
        size = size,
        duration = duration,
        mediaGroupId = mediaGroupId
    )

    fun MessageAttachment.toDto(): MessageAttachmentDto = MessageAttachmentDto(
        url = url,
        type = type.name.lowercase(),
        size = size,
        duration = duration,
        mediaGroupId = mediaGroupId
    )

    fun MessageDto.toDomain(): Message {
        val domainAttachments = attachments?.map { it.toDomain() } ?: emptyList()
        val derivedGroupId = domainAttachments.firstOrNull()?.mediaGroupId

        val baseType = try {
            MessageType.valueOf(messageType.uppercase())
        } catch (e: Exception) {
            when (messageType.lowercase()) {
                "text" -> MessageType.TEXT
                "voice" -> MessageType.VOICE
                "audio" -> MessageType.AUDIO
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "media_group" -> MessageType.MEDIA_GROUP
                "file" -> MessageType.FILE
                "contact" -> MessageType.CONTACT
                "location" -> MessageType.LOCATION
                "live_location" -> MessageType.LIVE_LOCATION
                "gif" -> MessageType.GIF
                "sticker" -> MessageType.STICKER
                "link_preview" -> MessageType.LINK_PREVIEW
                "poll" -> MessageType.POLL
                "music" -> MessageType.MUSIC
                "shared_post" -> MessageType.SHARED_POST
                "story_share" -> MessageType.STORY_SHARE
                "event" -> MessageType.EVENT
                "product" -> MessageType.PRODUCT
                "payment" -> MessageType.PAYMENT
                "map" -> MessageType.MAP
                "code_snippet" -> MessageType.CODE_SNIPPET
                "call" -> MessageType.CALL
                "system" -> MessageType.SYSTEM
                "ephemeral_media" -> MessageType.EPHEMERAL_MEDIA
                else -> MessageType.TEXT
            }
        }
        val finalType = if (domainAttachments.size >= 2 || baseType == MessageType.MEDIA_GROUP) MessageType.MEDIA_GROUP else baseType

        val mappedDeliveryStatus = when {
            deliveryStatus.equals("failed", ignoreCase = true) -> DeliveryStatus.FAILED
            readAt != null || (readBy?.any { it != senderId } == true) || deliveryStatus.equals("read", ignoreCase = true) -> DeliveryStatus.READ
            deliveredAt != null || deliveryStatus.equals("delivered", ignoreCase = true) -> DeliveryStatus.DELIVERED
            deliveryStatus.equals("sending", ignoreCase = true) -> DeliveryStatus.SENDING
            else -> DeliveryStatus.SENT
        }

        val mappedContentStatus = when {
            isDeleted || deletedAt != null || contentState.equals("deleted", ignoreCase = true) -> ContentStatus.DELETED
            isEdited || editedAt != null || contentState.equals("edited", ignoreCase = true) -> ContentStatus.EDITED
            else -> ContentStatus.ACTIVE
        }

        val parsedMetadata = metadata?.let {
            try {
                json.decodeFromString(MessageMetadataContainer.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }

        val isForwarded = parsedMetadata?.forwarded != null
        val forwardedFromMessageId = parsedMetadata?.forwarded?.originalMessageId

        return Message(
            id = id ?: "",
            chatId = chatId,
            senderId = senderId,
            content = content,
            messageType = finalType,
            mediaUrl = mediaUrl ?: domainAttachments.firstOrNull()?.url,
            deliveryStatus = mappedDeliveryStatus,
            contentStatus = mappedContentStatus,
            isDeleted = mappedContentStatus == ContentStatus.DELETED,
            isEdited = mappedContentStatus == ContentStatus.EDITED,
            isForwarded = isForwarded,
            replyToId = replyToId,
            forwardedFromMessageId = forwardedFromMessageId,
            sentAt = normalizeTimestamp(sentAt ?: createdAt),
            deliveredAt = normalizeTimestamp(deliveredAt),
            readAt = normalizeTimestamp(readAt),
            editedAt = normalizeTimestamp(editedAt),
            deletedAt = normalizeTimestamp(deletedAt),
            failureReason = failureReason,
            createdAt = normalizeTimestamp(createdAt) ?: "",
            updatedAt = normalizeTimestamp(updatedAt),
            readBy = readBy ?: emptyList(),
            expiresAt = normalizeTimestamp(expiresAt),
            encryptionFailureReason = encryptionFailureReason,
            attachments = domainAttachments,
            mediaGroupId = derivedGroupId,
            metadataContainer = parsedMetadata
        )
    }

    fun Message.mergeMonotonic(newer: Message): Message {
        val mergedDelivery = when {
            this.deliveryStatus == DeliveryStatus.FAILED && newer.deliveryStatus != DeliveryStatus.FAILED -> this.deliveryStatus
            this.deliveryStatus == DeliveryStatus.READ && newer.deliveryStatus != DeliveryStatus.FAILED -> DeliveryStatus.READ
            this.deliveryStatus == DeliveryStatus.DELIVERED && (newer.deliveryStatus == DeliveryStatus.SENT || newer.deliveryStatus == DeliveryStatus.SENDING) -> DeliveryStatus.DELIVERED
            else -> newer.deliveryStatus
        }

        val mergedSentAt = newer.sentAt ?: this.sentAt
        val mergedDeliveredAt = newer.deliveredAt ?: this.deliveredAt
        val mergedReadAt = newer.readAt ?: this.readAt
        val mergedEditedAt = newer.editedAt ?: this.editedAt
        val mergedDeletedAt = newer.deletedAt ?: this.deletedAt

        val mergedContentStatus = when {
            this.contentStatus == ContentStatus.DELETED || newer.contentStatus == ContentStatus.DELETED -> ContentStatus.DELETED
            newer.contentStatus == ContentStatus.EDITED || this.contentStatus == ContentStatus.EDITED -> ContentStatus.EDITED
            else -> ContentStatus.ACTIVE
        }

        val stableCreatedAt = if (this.createdAt.isNotBlank()) this.createdAt else newer.createdAt

        return newer.copy(
            clientId = this.clientId,
            createdAt = stableCreatedAt,
            deliveryStatus = mergedDelivery,
            contentStatus = mergedContentStatus,
            isDeleted = mergedContentStatus == ContentStatus.DELETED,
            isEdited = mergedContentStatus == ContentStatus.EDITED,
            sentAt = mergedSentAt,
            deliveredAt = mergedDeliveredAt,
            readAt = mergedReadAt,
            editedAt = mergedEditedAt,
            deletedAt = mergedDeletedAt,
            failureReason = newer.failureReason ?: this.failureReason
        )
    }
}
