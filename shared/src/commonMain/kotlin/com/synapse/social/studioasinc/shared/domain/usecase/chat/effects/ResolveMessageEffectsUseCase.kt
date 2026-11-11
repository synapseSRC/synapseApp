package com.synapse.social.studioasinc.shared.domain.usecase.chat.effects

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.MessageEffect
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import com.synapse.social.studioasinc.shared.domain.util.ChatEffectResolver

/** Resolves the effects to render for one message. Never fails: a plain message has no effects. */
class ResolveMessageEffectsUseCase {
    suspend operator fun invoke(
        messageText: String,
        wordEffects: List<WordEffect>
    ): Result<List<MessageEffect>> =
        Result.success(ChatEffectResolver.resolve(messageText = messageText, wordEffects = wordEffects))
}
