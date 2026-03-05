package com.synapse.social.studioasinc.shared.domain.usecase.blocking

import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository

/**
 * Use case for unblocking a user.
 * 
 * Handles the business logic for removing a block relationship.
 * Delegates the actual unblock operation to the BlockRepository.
 * 
 * @param blockRepository Repository for blocking operations
 */
class UnblockUserUseCase(
    private val blockRepository: BlockRepository
) {
    /**
     * Unblocks a previously blocked user.
     * 
     * @param targetUserId The ID of the user to unblock
     * @return Result containing Unit on success, or an error on failure
     */
    suspend operator fun invoke(targetUserId: String): Result<Unit> {
        return blockRepository.unblockUser(targetUserId)
    }
}
