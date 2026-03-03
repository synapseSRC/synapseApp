package com.synapse.social.studioasinc.shared.domain.model.chat

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
    val readBy: List<String> = emptyList()
) {
    fun isFromMe(currentUserId: String): Boolean = senderId == currentUserId
}

enum class DeliveryStatus {
    SENT, DELIVERED, READ
}
