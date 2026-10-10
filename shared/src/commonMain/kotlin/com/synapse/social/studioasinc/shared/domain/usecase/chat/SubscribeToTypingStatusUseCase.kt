package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class SubscribeToTypingStatusUseCase(private val repository: ChatRepository) {
    operator fun invoke(chatId: String): Flow<TypingStatus> = repository.subscribeToTypingStatus(chatId)
}
