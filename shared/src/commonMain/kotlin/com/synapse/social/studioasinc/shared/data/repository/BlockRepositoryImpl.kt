package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.datasource.SupabaseBlockDataSource
import com.synapse.social.studioasinc.shared.data.mapper.BlockMapper
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of BlockRepository using Supabase.
 * Coordinates between data source and domain layer.
 * 
 * This implementation bridges the domain and data layers, handling:
 * - Duplicate block prevention
 * - DTO to domain model mapping
 * - Coroutine context management
 * - Error propagation
 */
class BlockRepositoryImpl(
    private val dataSource: SupabaseBlockDataSource
) : BlockRepository {
    
    /**
     * Blocks a user by their ID.
     * Checks for existing blocks before creating new ones to prevent duplicates.
     * 
     * @param targetUserId The ID of the user to block
     * @return Result containing Unit on success, or error if already blocked or operation fails
     */
    override suspend fun blockUser(targetUserId: String): Result<Unit> = 
        withContext(Dispatchers.Default) {
            // Check if already blocked
            val isBlocked = dataSource.isUserBlocked(targetUserId)
                .getOrElse { return@withContext Result.failure(it) }
            
            if (isBlocked) {
                return@withContext Result.failure(
                    IllegalStateException("User is already blocked")
                )
            }
            
            // Create block record
            dataSource.createBlock(targetUserId)
                .map { Unit }
        }
    
    /**
     * Unblocks a previously blocked user.
     * 
     * @param targetUserId The ID of the user to unblock
     * @return Result containing Unit on success, or error if operation fails
     */
    override suspend fun unblockUser(targetUserId: String): Result<Unit> = 
        withContext(Dispatchers.Default) {
            dataSource.deleteBlock(targetUserId)
        }
    
    /**
     * Retrieves all users blocked by the current user.
     * Maps DTOs to domain models using BlockMapper.
     * 
     * @return Result containing list of BlockedUser domain models or error if operation fails
     */
    override suspend fun getBlockedUsers(): Result<List<BlockedUser>> = 
        withContext(Dispatchers.Default) {
            dataSource.getBlockedUsers()
                .map { dtos -> BlockMapper.toDomainList(dtos) }
        }
    
    /**
     * Checks if a specific user is blocked by the current user.
     * 
     * @param targetUserId The ID of the user to check
     * @return Result containing boolean (true if blocked, false otherwise) or error if operation fails
     */
    override suspend fun isUserBlocked(targetUserId: String): Result<Boolean> = 
        withContext(Dispatchers.Default) {
            dataSource.isUserBlocked(targetUserId)
        }
}
