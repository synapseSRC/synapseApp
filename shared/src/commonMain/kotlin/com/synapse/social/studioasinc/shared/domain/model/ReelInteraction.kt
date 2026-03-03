package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReelInteraction(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("reel_id") val reelId: String,
    @SerialName("interaction_type") val interactionType: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("anonymous_oppose") val anonymousOppose: Boolean = false
)
