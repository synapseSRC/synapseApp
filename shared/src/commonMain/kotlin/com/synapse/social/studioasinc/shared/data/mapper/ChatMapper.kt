package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType

object ChatMapper {
    fun ChatDto.toDomain(): ChatInfo = ChatInfo(
        id = id,
        name = name,
        description = description,
        avatarUrl = avatarUrl,
        isGroup = isGroup,
        createdBy = createdBy,
        onlyAdminsCanMessage = onlyAdminsCanMessage,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun MessageDto.toDomain(): Message = Message(
        id = id ?: "",
        chatId = chatId,
        senderId = senderId,
        content = content,
        messageType = when (messageType) {
            "image" -> MessageType.IMAGE
            "video" -> MessageType.VIDEO
            "audio" -> MessageType.AUDIO
            else -> MessageType.TEXT
        },
        mediaUrl = mediaUrl,
        deliveryStatus = when {
            readBy?.isNotEmpty() == true -> DeliveryStatus.READ
            deliveryStatus == "delivered" -> DeliveryStatus.DELIVERED
            else -> DeliveryStatus.SENT
        },
        isDeleted = isDeleted,
        isEdited = isEdited,
        replyToId = replyToId,
        createdAt = createdAt ?: "",
        updatedAt = updatedAt,
        readBy = readBy ?: emptyList(),
        isEncrypted = isEncrypted,
        encryptedContent = encryptedContent,
        expiresAt = expiresAt,
        encryptionFailureReason = encryptionFailureReason
    )
}
