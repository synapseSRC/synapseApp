package com.synapse.social.studioasinc.shared.domain.util

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.EmojiEffect
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.EmojiEffectCatalog
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.MessageEffect
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.MessageEffectSource
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect

/**
 * Turns a message's readable text plus the conversation's word effects into the effects to render.
 *
 * Pure and synchronous, and it never rewrites the message: the caller decides what the readable
 * text is, which keeps effects out of the encryption path. Encrypted or undecryptable content is
 * simply not passed in, and then no effect can fire for it.
 */
object ChatEffectResolver {

    /** The effects [messageText] should play, capped at [WordEffectRules.MAX_EFFECTS_PER_MESSAGE]. */
    fun resolve(
        messageText: String,
        wordEffects: List<WordEffect>,
        emojiEffects: List<EmojiEffect> = EmojiEffectCatalog.effects,
        limit: Int = WordEffectRules.MAX_EFFECTS_PER_MESSAGE
    ): List<MessageEffect> {
        if (messageText.isBlank() || limit <= 0) return emptyList()

        val matched = mutableListOf<MessageEffect>()

        wordEffects.forEach { effect ->
            if (!effect.isEnabled) return@forEach
            val trigger = WordEffectRules.normalizeTrigger(effect.trigger)
            if (trigger.isEmpty()) return@forEach
            if (containsWholeWord(messageText, trigger)) {
                matched += MessageEffect(
                    source = MessageEffectSource.WORD_TRIGGER,
                    emoji = effect.emoji,
                    animation = effect.animation,
                    trigger = effect.trigger
                )
            }
        }

        emojiEffects.forEach { effect ->
            if (containsEmoji(messageText, effect.emoji)) {
                matched += MessageEffect(
                    source = MessageEffectSource.EMOJI,
                    emoji = effect.emoji,
                    animation = effect.animation
                )
            }
        }

        // A configured rule and the built-in catalog can describe the same flourish: play it once.
        return matched
            .distinctBy { it.emoji to it.animation }
            .take(limit)
    }

    /** Case-insensitive whole-word match, so "hi" does not fire inside "this". */
    private fun containsWholeWord(messageText: String, trigger: String): Boolean {
        val haystack = messageText.lowercase()
        var index = haystack.indexOf(trigger)
        while (index >= 0) {
            val before = haystack.getOrNull(index - 1)
            val after = haystack.getOrNull(index + trigger.length)
            if (before.isNotLetterOrDigit() && after.isNotLetterOrDigit()) return true
            index = haystack.indexOf(trigger, startIndex = index + 1)
        }
        return false
    }

    /**
     * Emoji are compared with and without their variation selector, because the same heart reaches
     * the client as two different sequences depending on the keyboard that typed it.
     */
    private fun containsEmoji(messageText: String, emoji: String): Boolean {
        if (emoji.isEmpty()) return false
        if (messageText.contains(emoji)) return true
        val withoutSelector = emoji.replace(VARIATION_SELECTOR, "")
        if (withoutSelector.isEmpty() || withoutSelector == emoji) return false
        return messageText.contains(withoutSelector)
    }

    private fun Char?.isNotLetterOrDigit(): Boolean = this == null || !this.isLetterOrDigit()

    private const val VARIATION_SELECTOR = "️"
}
