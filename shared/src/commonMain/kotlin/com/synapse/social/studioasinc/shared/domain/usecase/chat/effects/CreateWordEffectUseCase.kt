package com.synapse.social.studioasinc.shared.domain.usecase.chat.effects

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import com.synapse.social.studioasinc.shared.domain.repository.ChatEffectsRepository
import com.synapse.social.studioasinc.shared.domain.util.WordEffectError
import com.synapse.social.studioasinc.shared.domain.util.WordEffectRules
import com.synapse.social.studioasinc.shared.domain.util.WordEffectValidationException

/**
 * Adds a word effect to a conversation.
 *
 * Validation and the duplicate check live here rather than in the screen or the backend, so every
 * entry point rejects the same input. The unique index on chat id plus lower(trigger) stays as the
 * last line of defence when two devices add the same trigger at the same time.
 */
class CreateWordEffectUseCase(private val repository: ChatEffectsRepository) {
    suspend operator fun invoke(
        chatId: String,
        trigger: String,
        emoji: String,
        animation: ChatEffectAnimation
    ): Result<WordEffect> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("chatId must not be blank"))

        WordEffectRules.validate(trigger, emoji)?.let { error ->
            return Result.failure(WordEffectValidationException(error))
        }

        val normalizedTrigger = WordEffectRules.normalizeTrigger(trigger)
        val existing = repository.getWordEffects(chatId)
        val known = existing.getOrDefault(emptyList())
        if (existing.isSuccess && WordEffectRules.isDuplicate(normalizedTrigger, known)) {
            return Result.failure(WordEffectValidationException(WordEffectError.DUPLICATE_TRIGGER))
        }

        return repository.createWordEffect(
            chatId = chatId,
            trigger = normalizedTrigger,
            emoji = WordEffectRules.normalizeEmoji(emoji),
            animation = animation
        )
    }
}
