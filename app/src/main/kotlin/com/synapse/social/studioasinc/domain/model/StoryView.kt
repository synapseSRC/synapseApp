package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class StoryView(
    val id: String? = null,
    @SerialName("story_id")
    val storyId: String,
    @SerialName("viewer_id")
    val viewerId: String,
    @SerialName("viewed_at")
    val viewedAt: String? = null
)



data class StoryViewWithUser(
    val storyView: StoryView,
    val viewer: User?
)
