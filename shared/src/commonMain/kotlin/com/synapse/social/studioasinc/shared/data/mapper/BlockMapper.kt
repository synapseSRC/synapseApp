package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.BlockWithUserDTO
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import com.synapse.social.studioasinc.shared.util.TimeProvider
import kotlin.time.Instant

/**
 * Maps between DTOs and domain models for blocking feature.
 */
object BlockMapper {
    
    fun toDomain(dto: BlockWithUserDTO): BlockedUser {
        val profile = dto.blockedUser
        return BlockedUser(
            id = dto.id,
            blockedUserId = dto.blockedId,
            blockedUsername = profile?.username?.takeIf { it.isNotBlank() },
            blockedUserAvatar = profile?.avatar?.takeIf { it.isNotBlank() },
            blockedAt = parseBlockedAt(dto.createdAt),
            // The row must show the account the user actually blocked, so the display name falls
            // back to the username only when no display name was returned.
            blockedDisplayName = profile?.displayName?.takeIf { it.isNotBlank() }
                ?: profile?.username?.takeIf { it.isNotBlank() }
        )
    }
    
    fun toDomainList(dtos: List<BlockWithUserDTO>): List<BlockedUser> {
        return dtos.map { toDomain(it) }
    }

    /**
     * A single unparsable `created_at` used to blank out the whole list, so the timestamp
     * degrades to "now" instead of failing the fetch.
     */
    private fun parseBlockedAt(raw: String): Instant =
        runCatching { Instant.parse(raw) }.getOrElse { TimeProvider.nowInstant() }
}
