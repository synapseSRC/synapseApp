package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

/** Persists the current user's notification mute setting for a chat. */
class SetConversationMuteStatusUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(chatId: String, isMuted: Boolean): Result<Unit> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("Chat ID cannot be blank"))
        return repository.setConversationMuteStatus(chatId, isMuted)
    }
}
