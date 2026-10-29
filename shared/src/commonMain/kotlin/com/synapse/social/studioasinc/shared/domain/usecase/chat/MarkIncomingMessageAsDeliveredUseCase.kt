package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

/**
 * Acknowledges arrival of an incoming message for conversations no chat screen is subscribed to.
 * A device never acknowledges its own messages, and an arrival already recorded is not re-sent.
 */
class MarkIncomingMessageAsDeliveredUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(message: Message): Result<Unit> {
        val currentUserId = repository.getCurrentUserId()
        if (currentUserId.isNullOrBlank()) return Result.success(Unit)
        if (message.senderId == currentUserId) return Result.success(Unit)
        if (message.deliveryStatus == DeliveryStatus.DELIVERED || message.deliveryStatus == DeliveryStatus.READ) {
            return Result.success(Unit)
        }
        if (message.chatId.equals(repository.getActiveChatId(), ignoreCase = true)) return Result.success(Unit)
        return repository.markMessagesAsDelivered(message.chatId)
    }
}
