package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class PopulateMessageReactionsUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messages: List<Message>): List<Message> {
        return repository.getReactionsForMessages(messages)
    }
}
