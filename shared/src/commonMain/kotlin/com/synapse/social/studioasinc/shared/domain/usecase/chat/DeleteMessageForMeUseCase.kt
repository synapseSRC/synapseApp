package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class DeleteMessageForMeUseCase (private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Result<Unit> = repository.deleteMessageForMe(messageId)
}
