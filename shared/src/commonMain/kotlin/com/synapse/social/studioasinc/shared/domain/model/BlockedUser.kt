package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing a blocked user relationship.
 * Pure Kotlin with no backend dependencies.
 */
data class BlockedUser(
    val id: String,
    val blockedUserId: String,
    val blockedUsername: String?,
    val blockedUserAvatar: String?,
    val blockedAt: Instant
)
