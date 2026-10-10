package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoryReaction(
    val id: String? = null,
    @SerialName("story_id")
    val storyId: String,
    @SerialName("user_id")
    val userId: String,
    val emoji: String,
    @SerialName("created_at")
    val createdAt: String? = null
)
