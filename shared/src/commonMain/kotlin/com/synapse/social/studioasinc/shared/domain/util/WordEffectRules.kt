package com.synapse.social.studioasinc.shared.domain.util

import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect

/** Why a word effect was rejected, so the UI can explain it in the user's own language. */
enum class WordEffectError {
    BLANK_TRIGGER,
    TRIGGER_TOO_LONG,
    TRIGGER_HAS_CONTROL_CHARACTERS,
    BLANK_EMOJI,
    EMOJI_TOO_LONG,
    DUPLICATE_TRIGGER
}

/** Raised by the word-effect use cases when a rule rejects the input. */
class WordEffectValidationException(val error: WordEffectError) :
    IllegalStateException("Word effect rejected: " + error.name)

/**
 * The rules a word effect has to satisfy before it is stored.
 *
 * Domain only: no backend and no UI types, so the Supabase implementation and any future backend
 * validate exactly the same way, and the screen can turn [WordEffectError] into a message.
 */
object WordEffectRules {
    /** Long enough for a phrase, short enough to stay a word effect. */
    const val MAX_TRIGGER_LENGTH = 32

    /** A few code points, so a multi-code-point emoji and its variation selector both fit. */
    const val MAX_EMOJI_LENGTH = 8

    /** Effects one message may play at once, so a long message cannot flood the timeline. */
    const val MAX_EFFECTS_PER_MESSAGE = 3

    /** Matching and storage both use this form, so one trigger is never stored twice. */
    fun normalizeTrigger(raw: String): String = raw.trim().lowercase()

    fun normalizeEmoji(raw: String): String = raw.trim()

    /** The first rule [trigger] and [emoji] break, or null when both are valid. */
    fun validate(trigger: String, emoji: String): WordEffectError? {
        val normalizedTrigger = normalizeTrigger(trigger)
        val normalizedEmoji = normalizeEmoji(emoji)
        return when {
            normalizedTrigger.isEmpty() -> WordEffectError.BLANK_TRIGGER
            normalizedTrigger.length > MAX_TRIGGER_LENGTH -> WordEffectError.TRIGGER_TOO_LONG
            normalizedTrigger.any { it.hasControlCode() } -> WordEffectError.TRIGGER_HAS_CONTROL_CHARACTERS
            normalizedEmoji.isEmpty() -> WordEffectError.BLANK_EMOJI
            normalizedEmoji.length > MAX_EMOJI_LENGTH -> WordEffectError.EMOJI_TOO_LONG
            else -> null
        }
    }

    /** True when [trigger] already exists in [existing], ignoring the row identified by [ignoreId]. */
    fun isDuplicate(trigger: String, existing: List<WordEffect>, ignoreId: String? = null): Boolean {
        val normalized = normalizeTrigger(trigger)
        if (normalized.isEmpty()) return false
        return existing.any { it.id != ignoreId && normalizeTrigger(it.trigger) == normalized }
    }

    /** Control characters would let a trigger break the row or the rendering, so they are refused. */
    private fun Char.hasControlCode(): Boolean = code < 0x20 || code == 0x7F
}
