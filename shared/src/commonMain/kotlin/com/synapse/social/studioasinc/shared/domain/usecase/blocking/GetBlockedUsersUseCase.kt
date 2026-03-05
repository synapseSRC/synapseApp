package com.synapse.social.studioasinc.shared.domain.usecase.blocking

import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository

/**
 * Use case for retrieving all blocked users.
 * 
 * Fetches the list of users blocked by the current user.
 * Delegates the retrieval operation to the BlockRepository.
 * 
 * @param blockRepository Repository for blocking operations
 */
class GetBlockedUsersUseCase(
    private val blockRepository: BlockRepository
) {
    /**
     * Retrieves all users blocked by the current user.
     * 
     * @return Result containing list of blocked users on success, or an error on failure
     */
    suspend operator fun invoke(): Result<List<BlockedUser>> {
        return blockRepository.getBlockedUsers()
    }
}
