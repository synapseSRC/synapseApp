package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class DeleteConversationUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<Unit> {
        if (chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))
        }
        return repository.deleteConversation(chatId)
    }
}
