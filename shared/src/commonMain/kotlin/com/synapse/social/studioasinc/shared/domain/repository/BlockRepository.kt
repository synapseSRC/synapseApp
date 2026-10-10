package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.BlockedUser

/**
 * Repository interface for user blocking operations.
 * Defines the contract for managing blocking relationships between users.
 * 
 * All methods return Result types for comprehensive error handling.
 */
interface BlockRepository {
    /**
     * Blocks a user by their user ID.
     * 
     * @param targetUserId The ID of the user to block
     * @return Result containing Unit on success, or an error on failure
     */
    suspend fun blockUser(targetUserId: String): Result<Unit>
    
    /**
     * Unblocks a previously blocked user.
     * 
     * @param targetUserId The ID of the user to unblock
     * @return Result containing Unit on success, or an error on failure
     */
    suspend fun unblockUser(targetUserId: String): Result<Unit>
    
    /**
     * Retrieves all users blocked by the current user.
     * 
     * @return Result containing list of blocked users or an error on failure
     */
    suspend fun getBlockedUsers(): Result<List<BlockedUser>>
    
    /**
     * Checks if a specific user is blocked by the current user.
     * 
     * @param targetUserId The ID of the user to check
     * @return Result containing boolean status (true if blocked) or an error on failure
     */
    suspend fun isUserBlocked(targetUserId: String): Result<Boolean>
}
