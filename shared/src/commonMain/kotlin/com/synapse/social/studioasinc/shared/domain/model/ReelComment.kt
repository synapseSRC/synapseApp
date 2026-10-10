package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReelComment(
    val id: String,
    @SerialName("reel_id") val reelId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,


    val userUsername: String? = null,
    val userAvatarUrl: String? = null
)
