package com.synapse.social.studioasinc.shared.domain.model.chat

import com.synapse.social.studioasinc.shared.domain.model.User

/** Public information returned only for a valid, active group invitation. */
data class GroupInvitePreview(
    val chatId: String,
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val requireJoinApproval: Boolean,
    val memberCount: Int
)

/** A real pending membership request with its public profile, when available. */
data class GroupJoinRequest(
    val id: String,
    val chatId: String,
    val user: User,
    val createdAt: String?
)

enum class GroupJoinOutcome {
    JOINED,
    REQUESTED,
    ALREADY_MEMBER
}
