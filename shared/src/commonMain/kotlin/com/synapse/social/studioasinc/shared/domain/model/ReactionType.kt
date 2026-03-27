package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReactionType(
    val displayName: String,
    val emoji: String
) {
    LIKE("Like", "❤️"),
    LOVE("Love", "😍"),
    HAHA("Haha", "😂"),
    WOW("Wow", "😮"),
    SAD("Sad", "😢"),
    ANGRY("Angry", "😡");

    companion object {
        fun fromString(value: String?): ReactionType {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: LIKE
        }
    }
}
