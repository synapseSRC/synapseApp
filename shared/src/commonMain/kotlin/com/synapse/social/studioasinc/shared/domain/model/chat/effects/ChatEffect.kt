package com.synapse.social.studioasinc.shared.domain.model.chat.effects

/**
 * The animations a chat effect can play.
 *
 * The catalog is deliberately small: every entry has to stay cheap on a long timeline, and every
 * entry has a static frame it can fall back to when the device asks for reduced motion. Growing it
 * is a product decision, not a per-feature one.
 */
enum class ChatEffectAnimation {
    GLOW,
    CONFETTI,
    SPARKLE,
    PULSE;

    companion object {
        /**
         * Parses a stored animation name.
         *
         * Unknown or missing values fall back to [GLOW], the calmest option, so a row written by a
         * newer client never breaks an older one.
         */
        fun fromWire(value: String?): ChatEffectAnimation {
            val normalized = value?.trim() ?: return GLOW
            return entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) } ?: GLOW
        }
    }
}

/** Why an effect matched a message. */
enum class MessageEffectSource {
    /** A word or phrase this conversation configured. */
    WORD_TRIGGER,
    /** An emoji recognized in the message itself. */
    EMOJI
}

/**
 * A word-triggered effect configured for one conversation.
 *
 * Effects are conversation-scoped on purpose: sender and recipients read the same rows, so the same
 * message plays the same effect on both sides without the message itself, or the encryption
 * pipeline, being touched.
 *
 * [trigger] is stored normalized (trimmed, lower case), so matching never depends on how the user
 * typed it. [createdBy] is kept so a group can tell who added a rule.
 */
data class WordEffect(
    val id: String,
    val chatId: String,
    val trigger: String,
    val emoji: String,
    val animation: ChatEffectAnimation,
    val isEnabled: Boolean = true,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * An emoji that always carries a visual effect, wherever it appears.
 *
 * These ship with the app rather than being stored per conversation: they are the same for
 * everyone and are not configurable.
 */
data class EmojiEffect(
    val emoji: String,
    val animation: ChatEffectAnimation
)

/** One effect resolved for a concrete message, ready to be rendered. */
data class MessageEffect(
    val source: MessageEffectSource,
    val emoji: String,
    val animation: ChatEffectAnimation,
    /** The configured trigger for [MessageEffectSource.WORD_TRIGGER]; null for emoji effects. */
    val trigger: String? = null
)
