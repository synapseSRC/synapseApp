package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.BlockWithUserDTO
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import kotlinx.datetime.Instant

/**
 * Maps between DTOs and domain models for blocking feature.
 */
object BlockMapper {
    
    fun toDomain(dto: BlockWithUserDTO): BlockedUser {
        return BlockedUser(
            id = dto.id,
            blockedUserId = dto.blockedId,
            blockedUsername = dto.blockedUser?.username,
            blockedUserAvatar = dto.blockedUser?.avatar,
            blockedAt = Instant.parse(dto.createdAt)
        )
    }
    
    fun toDomainList(dtos: List<BlockWithUserDTO>): List<BlockedUser> {
        return dtos.map { toDomain(it) }
    }
}
