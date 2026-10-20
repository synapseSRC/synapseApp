package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class GetConversationsUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(forceNetwork: Boolean = false): Result<List<Conversation>> = repository.getConversations(forceNetwork)
}
