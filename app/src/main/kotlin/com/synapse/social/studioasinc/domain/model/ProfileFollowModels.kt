package com.synapse.social.studioasinc.domain.model

enum class ProfileFollowStatus {
    NONE,
    REQUESTED,
    FOLLOWING
}

data class ProfileFollowRequest(
    val requesterId: String,
    val username: String,
    val displayName: String?,
    val avatar: String?,
    val requestedAt: String?
)
