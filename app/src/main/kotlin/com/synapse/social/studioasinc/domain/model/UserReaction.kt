package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserReaction(
    @SerialName("user_id")
    val userId: String,
    val username: String,
    @SerialName("profile_image")
    val profileImage: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("reaction_type")
    val reactionType: String = "LIKE",
    @SerialName("reacted_at")
    val reactedAt: String? = null
) {
    fun getDisplayName(): String {
        return "@$username"
    }
}

