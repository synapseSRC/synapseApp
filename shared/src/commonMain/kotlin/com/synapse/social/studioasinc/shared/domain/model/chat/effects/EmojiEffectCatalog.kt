package com.synapse.social.studioasinc.shared.domain.model.chat.effects

/**
 * The built-in emoji effects.
 *
 * Kept to four entries on purpose: enough to make a message feel alive, small enough to stay
 * predictable and to keep the per-message animation budget. Order is the order the UI shows.
 */
object EmojiEffectCatalog {
    val effects: List<EmojiEffect> = listOf(
        EmojiEffect(emoji = "🎉", animation = ChatEffectAnimation.CONFETTI),
        EmojiEffect(emoji = "✨", animation = ChatEffectAnimation.SPARKLE),
        EmojiEffect(emoji = "❤️", animation = ChatEffectAnimation.PULSE),
        EmojiEffect(emoji = "🔥", animation = ChatEffectAnimation.GLOW)
    )

    /** The glyphs, in catalog order, for a picker that only needs the emoji. */
    val emojis: List<String> = effects.map { it.emoji }
}
