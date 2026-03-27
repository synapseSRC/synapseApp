package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class EditMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String, newContent: String): Result<Unit> =
        repository.editMessage(messageId, newContent)
}
