package com.synapse.social.studioasinc.shared.domain.model

import kotlin.time.Instant

/**
 * Domain model representing a blocked user relationship.
 * Pure Kotlin with no backend dependencies.
 */
data class BlockedUser(
    val id: String,
    val blockedUserId: String,
    val blockedUsername: String?,
    val blockedUserAvatar: String?,
    val blockedAt: Instant,
    /**
     * Best available human-readable name for the blocked account, resolved from the profile when
     * it can be loaded. Null only when the profile genuinely could not be retrieved.
     */
    val blockedDisplayName: String? = null
)
