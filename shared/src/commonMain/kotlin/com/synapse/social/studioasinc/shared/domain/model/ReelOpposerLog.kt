package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReelOpposerLog(
    val id: String,
    @SerialName("reel_id") val reelId: String,
    @SerialName("opposer_user_id") val opposerUserId: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    @SerialName("conversation_started") val conversationStarted: Boolean,
    @SerialName("created_at") val createdAt: String
)
