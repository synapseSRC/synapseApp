package com.synapse.social.studioasinc.shared.domain.usecase.blocking

import com.synapse.social.studioasinc.shared.domain.model.BlockStatus
import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository

/**
 * Use case for resolving the block relationship with another account in both directions.
 *
 * The messaging layer and the conversation screen both need to know whether the authenticated
 * account blocked the other account, whether it was blocked by it, or both. An unknown or empty
 * target resolves to [BlockStatus.NONE] so a missing participant id cannot accidentally lock a
 * conversation.
 *
 * @param blockRepository Repository for blocking operations
 */
class GetBlockStatusUseCase(
    private val blockRepository: BlockRepository
) {
    /**
     * Resolves the block relationship with [otherUserId].
     *
     * @param otherUserId The account to inspect
     * @return Result containing the directional block status, or an error on failure
     */
    suspend operator fun invoke(otherUserId: String): Result<BlockStatus> {
        if (otherUserId.isBlank()) return Result.success(BlockStatus.NONE)
        return blockRepository.getBlockStatus(otherUserId)
    }
}
