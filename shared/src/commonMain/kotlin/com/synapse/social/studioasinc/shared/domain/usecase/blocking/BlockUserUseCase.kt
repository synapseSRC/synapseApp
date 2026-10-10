package com.synapse.social.studioasinc.shared.domain.usecase.blocking

import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase

/**
 * Use case for blocking a user.
 * 
 * Validates business rules:
 * - User must be authenticated
 * - User cannot block themselves
 * 
 * Delegates actual blocking operation to the BlockRepository.
 * 
 * @param blockRepository Repository for blocking operations
 * @param getCurrentUserIdUseCase Use case to retrieve current user ID
 */
class BlockUserUseCase(
    private val blockRepository: BlockRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    /**
     * Blocks a target user.
     * 
     * @param targetUserId The ID of the user to block
     * @return Result containing Unit on success, or an error on failure
     */
    suspend operator fun invoke(targetUserId: String): Result<Unit> {
        // Get current user ID
        val currentUserId = getCurrentUserIdUseCase() 
            ?: return Result.failure(IllegalStateException("User not authenticated"))
        
        // Validate user is not blocking themselves
        if (currentUserId == targetUserId) {
            return Result.failure(IllegalArgumentException("Cannot block yourself"))
        }
        
        // Delegate to repository
        return blockRepository.blockUser(targetUserId)
    }
}
