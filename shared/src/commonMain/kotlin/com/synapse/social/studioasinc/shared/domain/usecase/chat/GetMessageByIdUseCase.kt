package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetMessageByIdUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Result<Message?> = repository.getMessageById(messageId)
}
