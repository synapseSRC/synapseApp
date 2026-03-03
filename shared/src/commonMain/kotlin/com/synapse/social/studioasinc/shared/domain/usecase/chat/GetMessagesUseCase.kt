package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetMessagesUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>> =
        repository.getMessages(chatId, limit, before)
}
