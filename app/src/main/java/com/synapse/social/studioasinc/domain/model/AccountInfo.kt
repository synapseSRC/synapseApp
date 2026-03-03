package com.synapse.social.studioasinc.domain.model

import kotlinx.datetime.Instant

enum class SubscriptionType {
    FREE, PLUS
}

data class AccountInfo(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String?,
    val bio: String?,
    val accountType: SubscriptionType,
    val isVerified: Boolean,
    val createdAt: String?,
    val lastLoginAt: Instant?,
    val postsCount: Int,
    val followersCount: Int,
    val followingCount: Int,
    val storiesCount: Int,
    val reelsCount: Int,
    val region: String,
    val language: String
)
