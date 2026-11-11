package com.synapse.social.studioasinc.shared.domain.usecase.chat.effects

import com.synapse.social.studioasinc.shared.domain.repository.ChatEffectsRepository

/** Removes a word effect from a conversation. */
class DeleteWordEffectUseCase(private val repository: ChatEffectsRepository) {
    suspend operator fun invoke(chatId: String, effectId: String): Result<Unit> {
        if (chatId.isBlank() || effectId.isBlank()) return Result.success(Unit)
        return repository.deleteWordEffect(chatId, effectId)
    }
}
