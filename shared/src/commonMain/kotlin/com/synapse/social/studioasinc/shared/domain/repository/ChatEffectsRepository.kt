package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect

/**
 * Conversation-scoped chat effects.
 *
 * Provider-neutral by design: implementations own their backend (Supabase today) and return a
 * [Result], so the UI can tell "this chat has no effects yet" from "the backend could not be
 * reached".
 */
interface ChatEffectsRepository {
    /** Word effects configured for [chatId], ordered for display. */
    suspend fun getWordEffects(chatId: String): Result<List<WordEffect>>

    /** Adds a word effect to [chatId]. [trigger] is expected already normalized. */
    suspend fun createWordEffect(
        chatId: String,
        trigger: String,
        emoji: String,
        animation: ChatEffectAnimation
    ): Result<WordEffect>

    /** Replaces a stored word effect, including turning it on or off. */
    suspend fun updateWordEffect(effect: WordEffect): Result<WordEffect>

    suspend fun deleteWordEffect(chatId: String, effectId: String): Result<Unit>
}
