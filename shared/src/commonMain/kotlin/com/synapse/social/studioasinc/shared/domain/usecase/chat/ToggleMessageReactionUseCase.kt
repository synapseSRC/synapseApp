package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class ToggleMessageReactionUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(messageId: String, emoji: String): Result<Unit> {
        return repository.toggleMessageReaction(messageId, emoji)
    }
}
