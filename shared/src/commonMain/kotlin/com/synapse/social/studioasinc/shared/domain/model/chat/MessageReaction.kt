package com.synapse.social.studioasinc.shared.domain.model.chat

data class MessageReaction(
    val messageId: String,
    val userId: String,
    val reactionEmoji: String,
    val timestamp: Long
)
