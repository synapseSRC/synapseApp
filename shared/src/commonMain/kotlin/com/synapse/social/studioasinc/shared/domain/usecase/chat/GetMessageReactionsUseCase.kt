package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetMessageReactionsUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Result<List<MessageReaction>> {
        return repository.getReactionsForMessage(messageId)
    }
}
