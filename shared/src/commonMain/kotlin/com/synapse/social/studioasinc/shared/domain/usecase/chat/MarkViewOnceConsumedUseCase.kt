package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class MarkViewOnceConsumedUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return repository.markViewOnceConsumed(messageId)
    }
}