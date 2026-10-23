package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

/**
 * Reconciles a real-time WebSocket-delivered message into the local cache.
 *
 * This use case ensures real-time messages go through the same cache coherence logic
 * as background sync, preventing split-brain state where a message appears in the inbox
 * preview but not in the active chat screen.
 *
 * Returns the final message that should be displayed in the UI.
 */
class ReconcileRealtimeMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, message: Message): Message =
        repository.reconcileRealtimeMessage(chatId, message)
}
