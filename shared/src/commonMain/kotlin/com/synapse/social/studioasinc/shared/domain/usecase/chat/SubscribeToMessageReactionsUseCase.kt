package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class SubscribeToMessageReactionsUseCase(private val repository: ChatRepository) {
    operator fun invoke(): Flow<MessageReaction> {
        return repository.subscribeToMessageReactions()
    }
}
