package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class DeleteMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Result<Unit> = repository.deleteMessage(messageId)
}
