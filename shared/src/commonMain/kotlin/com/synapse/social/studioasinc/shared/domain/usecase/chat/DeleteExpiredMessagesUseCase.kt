package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class DeleteExpiredMessagesUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<Int> {
        return repository.deleteExpiredMessages(chatId)
    }
}
