package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class BroadcastTypingStatusUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, isTyping: Boolean): Result<Unit> =
        repository.broadcastTypingStatus(chatId, isTyping)
}
