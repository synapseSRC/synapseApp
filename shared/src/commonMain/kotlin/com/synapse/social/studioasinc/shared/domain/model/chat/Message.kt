package com.synapse.social.studioasinc.shared.domain.model.chat

import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val messageType: MessageType,
    val mediaUrl: String? = null,
    val deliveryStatus: DeliveryStatus,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val replyToId: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val readBy: List<String> = emptyList(),
    val isEncrypted: Boolean = false,
    val encryptedContent: String? = null,
    val expiresAt: String? = null,
    val encryptionFailureReason: String? = null,
    val reactions: Map<ReactionType, Int> = emptyMap(),
    val userReaction: ReactionType? = null,
    val linkPreview: LinkPreview? = null
) {
    fun isFromMe(currentUserId: String): Boolean = senderId == currentUserId
}

enum class DeliveryStatus {
    SENT, DELIVERED, READ
}
