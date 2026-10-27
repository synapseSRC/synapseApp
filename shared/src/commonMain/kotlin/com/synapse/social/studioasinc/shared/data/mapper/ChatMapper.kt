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
import com.synapse.social.studioasinc.shared.domain.util.EncryptedPayloadDetector
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

    fun parseInstant(raw: String?): kotlinx.datetime.Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            val normalized = normalizeTimestamp(raw) ?: raw
            kotlinx.datetime.Instant.parse(normalized)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Canonical chronological comparator for chat messages.
     * Compares parsed Instant objects down to nanosecond precision first,
     * falling back to raw timestamp strings, server id, and clientId.
     * Guarantees strict, deterministic total ordering across all platforms.
     */
    val MessageChronologicalComparator: Comparator<Message> = Comparator { a, b ->
        val instantA = parseInstant(a.createdAt)
        val instantB = parseInstant(b.createdAt)
        if (instantA != null && instantB != null) {
            val instantCompare = instantA.compareTo(instantB)
            if (instantCompare != 0) return@Comparator instantCompare
        } else {
            val strCompare = (a.createdAt ?: "").compareTo(b.createdAt ?: "")
            if (strCompare != 0) return@Comparator strCompare
        }
        val idCompare = a.id.compareTo(b.id)
        if (idCompare != 0) return@Comparator idCompare
        a.clientId.compareTo(b.clientId)
    }

    fun insertChronologically(current: List<Message>, message: Message): List<Message> {
        val ordering = MessageChronologicalComparator
        val last = current.lastOrNull()
        if (last == null || ordering.compare(last, message) <= 0) return current + message

        var low = 0
        var high = current.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (ordering.compare(current[middle], message) <= 0) low = middle + 1 else high = middle
        }
        return current.toMutableList().apply { add(low, message) }
    }

    fun replaceChronologically(current: List<Message>, targetId: String, message: Message): List<Message> {
        val index = current.indexOfFirst { it.id == targetId }
        if (index < 0) return insertChronologically(current, message)
        val ordering = MessageChronologicalComparator
        val previous = current.getOrNull(index - 1)
        val next = current.getOrNull(index + 1)
        if ((previous == null || ordering.compare(previous, message) <= 0) &&
            (next == null || ordering.compare(message, next) <= 0)
        ) {
            return current.toMutableList().apply { set(index, message) }
        }
        return insertChronologically(current.toMutableList().apply { removeAt(index) }, message)
    }

    /**
     * Merges a fetched page or realtime batch into the visible timeline. Messages are matched
     * by id first and by stable clientId second, so a server confirmation replaces the
     * optimistic copy instead of duplicating it, and the result is always chronologically sorted.
     */
    fun mergeChronologically(current: List<Message>, incoming: List<Message>): List<Message> {
        if (incoming.isEmpty()) return current
        val ordering = MessageChronologicalComparator

        val currentByClientId = current.filter { it.clientId.isNotBlank() }.associateBy { it.clientId }

        val mergedMap = LinkedHashMap<String, Message>()
        current.forEach { msg -> mergedMap[msg.id] = msg }

        incoming.forEach { fresh ->
            val existingById = mergedMap[fresh.id]
            val existingByClient = if (fresh.clientId.isNotBlank()) currentByClientId[fresh.clientId] else null
            val existing = existingById ?: existingByClient

            if (existing != null) {
                val merged = existing.mergeMonotonic(fresh)
                if (existing.id != fresh.id) {
                    mergedMap.remove(existing.id)
                }
                mergedMap[merged.id] = merged
            } else {
                mergedMap[fresh.id] = fresh
            }
        }

        return mergedMap.values.sortedWith(ordering)
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
        allowMemberSettingsEdits = allowMemberSettingsEdits,
        requireJoinApproval = requireJoinApproval,
        allowMemberInvites = allowMemberInvites,
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

        fun hasDeliveryState(state: String): Boolean =
            deliveryStatus.equals(state, ignoreCase = true) || messageState.equals(state, ignoreCase = true)

        val mappedDeliveryStatus = when {
            readAt != null || (readBy?.any { it.isNotBlank() && it != senderId } == true) || hasDeliveryState("read") -> DeliveryStatus.READ
            deliveredAt != null || hasDeliveryState("delivered") -> DeliveryStatus.DELIVERED
            hasDeliveryState("failed") -> DeliveryStatus.FAILED
            hasDeliveryState("sending") -> DeliveryStatus.SENDING
            else -> DeliveryStatus.SENT
        }

        val isDeletedMessage = isDeleted || deletedAt != null || contentState.equals("deleted", ignoreCase = true)

        val mappedContentStatus = when {
            isDeletedMessage -> ContentStatus.DELETED
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
            content = if (isDeletedMessage) "" else content,
            messageType = finalType,
            mediaUrl = if (isDeletedMessage) null else mediaUrl ?: domainAttachments.firstOrNull()?.url,
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
            failureReason = failureReason.takeIf { mappedDeliveryStatus == DeliveryStatus.FAILED },
            createdAt = normalizeTimestamp(createdAt) ?: "",
            updatedAt = normalizeTimestamp(updatedAt),
            readBy = readBy ?: emptyList(),
            expiresAt = normalizeTimestamp(expiresAt),
            encryptionFailureReason = encryptionFailureReason,
            attachments = if (isDeletedMessage) emptyList() else domainAttachments,
            mediaGroupId = derivedGroupId,
            metadataContainer = if (isDeletedMessage) null else parsedMetadata,
            clientId = clientId ?: id ?: "",
            deviceId = deviceId
        )
    }

    fun Message.mergeMonotonic(newer: Message): Message {
        val mergedSentAt = newer.sentAt ?: this.sentAt
        val mergedDeliveredAt = newer.deliveredAt ?: this.deliveredAt
        val mergedReadAt = newer.readAt ?: this.readAt
        val mergedReadBy = (this.readBy + newer.readBy).filter { it.isNotBlank() }.distinct()

        // Optimistic sentAt is not confirmation; receipt evidence must survive stale events.
        val mergedDelivery = when {
            mergedReadAt != null || mergedReadBy.any { it != newer.senderId } ||
                this.deliveryStatus == DeliveryStatus.READ || newer.deliveryStatus == DeliveryStatus.READ -> DeliveryStatus.READ
            mergedDeliveredAt != null || this.deliveryStatus == DeliveryStatus.DELIVERED ||
                newer.deliveryStatus == DeliveryStatus.DELIVERED -> DeliveryStatus.DELIVERED
            this.deliveryStatus == DeliveryStatus.SENT || newer.deliveryStatus == DeliveryStatus.SENT -> DeliveryStatus.SENT
            else -> newer.deliveryStatus
        }

        // A fetch or realtime event can deliver a still-encrypted payload after this message
        // was already decrypted (or while decryption temporarily failed). Never let that
        // overwrite readable content and the fields carried inside the encrypted payload.
        val incomingDisplayable = !EncryptedPayloadDetector.isUndecryptable(newer.content, newer.encryptionFailureReason)
        val existingDisplayable = !EncryptedPayloadDetector.isUndecryptable(this.content, this.encryptionFailureReason)
        val keepExistingContent = !incomingDisplayable && existingDisplayable

        val incomingDeleted = newer.isDeleted || newer.contentStatus == ContentStatus.DELETED
        val mergedContentStatus = when {
            incomingDeleted || this.contentStatus == ContentStatus.DELETED -> ContentStatus.DELETED
            keepExistingContent -> this.contentStatus
            newer.contentStatus == ContentStatus.EDITED || this.contentStatus == ContentStatus.EDITED -> ContentStatus.EDITED
            else -> ContentStatus.ACTIVE
        }

        val mergedEditedAt = if (keepExistingContent) this.editedAt ?: newer.editedAt else newer.editedAt ?: this.editedAt
        val mergedDeletedAt = newer.deletedAt ?: this.deletedAt

        // Authoritative server timestamp from newer takes priority over temporary optimistic timestamp.
        val stableCreatedAt = if (newer.createdAt.isNotBlank()) newer.createdAt else this.createdAt

        return newer.copy(
            clientId = this.clientId,
            messageType = if (keepExistingContent) this.messageType else newer.messageType,
            content = if (keepExistingContent) this.content else newer.content,
            mediaUrl = if (keepExistingContent) this.mediaUrl ?: newer.mediaUrl else newer.mediaUrl ?: this.mediaUrl,
            attachments = if (keepExistingContent && this.attachments.isNotEmpty()) this.attachments else newer.attachments,
            mediaGroupId = if (keepExistingContent) this.mediaGroupId ?: newer.mediaGroupId else newer.mediaGroupId ?: this.mediaGroupId,
            metadataContainer = if (keepExistingContent) this.metadataContainer ?: newer.metadataContainer else newer.metadataContainer ?: this.metadataContainer,
            linkPreview = if (keepExistingContent) this.linkPreview ?: newer.linkPreview else newer.linkPreview ?: this.linkPreview,
            replyToId = if (keepExistingContent) this.replyToId ?: newer.replyToId else newer.replyToId ?: this.replyToId,
            isForwarded = if (keepExistingContent) this.isForwarded else newer.isForwarded,
            forwardedFromMessageId = if (keepExistingContent) this.forwardedFromMessageId ?: newer.forwardedFromMessageId else newer.forwardedFromMessageId ?: this.forwardedFromMessageId,
            createdAt = stableCreatedAt,
            updatedAt = newer.updatedAt ?: this.updatedAt,
            expiresAt = newer.expiresAt ?: this.expiresAt,
            deliveryStatus = mergedDelivery,
            contentStatus = mergedContentStatus,
            isDeleted = mergedContentStatus == ContentStatus.DELETED,
            isEdited = mergedContentStatus == ContentStatus.EDITED,
            sentAt = mergedSentAt,
            deliveredAt = mergedDeliveredAt,
            readAt = mergedReadAt,
            readBy = mergedReadBy,
            editedAt = mergedEditedAt,
            deletedAt = mergedDeletedAt,
            encryptionFailureReason = if (keepExistingContent) this.encryptionFailureReason else newer.encryptionFailureReason,
            failureReason = if (mergedDelivery == DeliveryStatus.FAILED) newer.failureReason ?: this.failureReason else null
        )
    }
}
