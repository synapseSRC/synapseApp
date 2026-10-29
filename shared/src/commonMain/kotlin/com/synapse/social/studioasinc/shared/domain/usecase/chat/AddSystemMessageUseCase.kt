package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class AddSystemMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, content: String): Result<Message> {
        return repository.addSystemMessage(chatId, content)
    }
}
