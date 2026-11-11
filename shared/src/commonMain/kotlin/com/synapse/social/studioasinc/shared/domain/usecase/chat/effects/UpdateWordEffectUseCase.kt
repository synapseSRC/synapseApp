package com.synapse.social.studioasinc.shared.domain.usecase.chat.effects

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import com.synapse.social.studioasinc.shared.domain.repository.ChatEffectsRepository
import com.synapse.social.studioasinc.shared.domain.util.WordEffectError
import com.synapse.social.studioasinc.shared.domain.util.WordEffectRules
import com.synapse.social.studioasinc.shared.domain.util.WordEffectValidationException

/** Rewrites a stored word effect, including enabling or disabling it. */
class UpdateWordEffectUseCase(private val repository: ChatEffectsRepository) {
    suspend operator fun invoke(effect: WordEffect): Result<WordEffect> {
        if (effect.id.isBlank() || effect.chatId.isBlank()) {
            return Result.failure(IllegalArgumentException("effect id and chatId are required"))
        }

        WordEffectRules.validate(effect.trigger, effect.emoji)?.let { error ->
            return Result.failure(WordEffectValidationException(error))
        }

        val normalizedTrigger = WordEffectRules.normalizeTrigger(effect.trigger)
        val existing = repository.getWordEffects(effect.chatId)
        val known = existing.getOrDefault(emptyList())
        if (existing.isSuccess && WordEffectRules.isDuplicate(normalizedTrigger, known, ignoreId = effect.id)) {
            return Result.failure(WordEffectValidationException(WordEffectError.DUPLICATE_TRIGGER))
        }

        return repository.updateWordEffect(
            effect.copy(
                trigger = normalizedTrigger,
                emoji = WordEffectRules.normalizeEmoji(effect.emoji)
            )
        )
    }
}
