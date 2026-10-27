package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reaction(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("target_id")
    val targetId: String,
    @SerialName("target_type")
    val targetType: String = "post",
    @SerialName("reaction_type")
    val reactionType: String = "LIKE",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
}

