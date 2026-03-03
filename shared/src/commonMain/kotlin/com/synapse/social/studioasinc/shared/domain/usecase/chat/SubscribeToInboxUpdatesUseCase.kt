package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class SubscribeToInboxUpdatesUseCase(private val repository: ChatRepository) {
    operator fun invoke(chatIds: List<String>): Flow<Message> = repository.subscribeToInboxUpdates(chatIds)
}
