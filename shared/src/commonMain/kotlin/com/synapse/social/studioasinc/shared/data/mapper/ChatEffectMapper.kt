package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.chat.effects.WordEffectDto
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect

/** Maps between the chat_word_effects schema and the domain model. */
object ChatEffectMapper {

    fun WordEffectDto.toDomain(): WordEffect = WordEffect(
        id = id.orEmpty(),
        chatId = chatId,
        trigger = trigger,
        emoji = emoji,
        animation = ChatEffectAnimation.fromWire(animation),
        isEnabled = isEnabled,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun WordEffect.toDto(): WordEffectDto = WordEffectDto(
        id = id.takeIf { it.isNotBlank() },
        chatId = chatId,
        trigger = trigger,
        emoji = emoji,
        animation = animation.name,
        isEnabled = isEnabled,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
