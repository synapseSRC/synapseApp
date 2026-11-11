package com.synapse.social.studioasinc.shared.domain.usecase.chat.effects

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import com.synapse.social.studioasinc.shared.domain.repository.ChatEffectsRepository

/** Reads the word effects a conversation has configured. */
class GetWordEffectsUseCase(private val repository: ChatEffectsRepository) {
    suspend operator fun invoke(chatId: String): Result<List<WordEffect>> {
        if (chatId.isBlank()) return Result.success(emptyList())
        return repository.getWordEffects(chatId)
    }
}
