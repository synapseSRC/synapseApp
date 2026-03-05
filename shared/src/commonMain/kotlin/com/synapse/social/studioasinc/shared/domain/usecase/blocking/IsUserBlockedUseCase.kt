package com.synapse.social.studioasinc.shared.domain.usecase.blocking

import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository

/**
 * Use case for checking if a user is blocked.
 * 
 * Determines whether a specific user is currently blocked by the current user.
 * Delegates the check operation to the BlockRepository.
 * 
 * @param blockRepository Repository for blocking operations
 */
class IsUserBlockedUseCase(
    private val blockRepository: BlockRepository
) {
    /**
     * Checks if a specific user is blocked.
     * 
     * @param targetUserId The ID of the user to check
     * @return Result containing true if blocked, false if not blocked, or an error on failure
     */
    suspend operator fun invoke(targetUserId: String): Result<Boolean> {
        return blockRepository.isUserBlocked(targetUserId)
    }
}
