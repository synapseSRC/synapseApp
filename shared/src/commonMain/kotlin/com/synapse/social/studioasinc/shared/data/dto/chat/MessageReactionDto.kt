package com.synapse.social.studioasinc.shared.data.dto.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("reaction_type") val reactionEmoji: String,
    @SerialName("created_at") val createdAt: String? = null
)
