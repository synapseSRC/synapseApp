package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.BlockDTO
import com.synapse.social.studioasinc.shared.data.dto.BlockWithUserDTO
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data source for blocking operations using Supabase.
 * Handles all direct communication with Supabase backend.
 */
class SupabaseBlockDataSource(
    private val client: SupabaseClient
) {
    private val tableName = "blocks"
    
    /**
     * Creates a new block record in Supabase.
     * Checks for existing block before creating to prevent duplicates.
     */
    suspend fun createBlock(targetUserId: String): Result<BlockDTO> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id 
                ?: throw IllegalStateException("User not authenticated")
            
            // Check for existing block first
            val existingBlock = client.postgrest[tableName]
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("blocker_id", currentUserId)
                        eq("blocked_id", targetUserId)
                    }
                    limit(1)
                }
                .decodeList<BlockDTO>()
            
            if (existingBlock.isNotEmpty()) {
                throw IllegalStateException("User is already blocked")
            }
            
            // Create new block record
            val blockData = mapOf(
                "blocker_id" to currentUserId,
                "blocked_id" to targetUserId
            )
            
            client.postgrest[tableName]
                .insert(blockData) {
                    select()
                }
                .decodeSingle<BlockDTO>()
        }.onFailure { error ->
            Napier.e("Error creating block for user $targetUserId", error)
        }
    }
    
    /**
     * Deletes a block record from Supabase.
     */
    suspend fun deleteBlock(targetUserId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id 
                ?: throw IllegalStateException("User not authenticated")
            
            client.postgrest[tableName]
                .delete {
                    filter {
                        eq("blocker_id", currentUserId)
                        eq("blocked_id", targetUserId)
                    }
                }
            Unit
        }.onFailure { error ->
            Napier.e("Error deleting block for user $targetUserId", error)
        }
    }
    
    /**
     * Fetches all blocks for the current user with user profile details.
     * Joins with users table to get profile information.
     */
    suspend fun getBlockedUsers(): Result<List<BlockWithUserDTO>> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id 
                ?: throw IllegalStateException("User not authenticated")
            
            // Fetch blocks with joined user profile data
            client.postgrest[tableName]
                .select(
                    columns = Columns.raw(
                        """
                        id,
                        blocker_id,
                        blocked_id,
                        created_at,
                        blocked_user:users!blocked_id(uid, username, avatar)
                        """.trimIndent()
                    )
                ) {
                    filter {
                        eq("blocker_id", currentUserId)
                    }
                }
                .decodeList<BlockWithUserDTO>()
        }.onFailure { error ->
            Napier.e("Error fetching blocked users", error)
        }
    }
    
    /**
     * Checks if a specific user is blocked.
     */
    suspend fun isUserBlocked(targetUserId: String): Result<Boolean> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = client.auth.currentUserOrNull()?.id 
                ?: throw IllegalStateException("User not authenticated")
            
            val count = client.postgrest[tableName]
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("blocker_id", currentUserId)
                        eq("blocked_id", targetUserId)
                    }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }
                .countOrNull() ?: 0
            
            count > 0
        }.onFailure { error ->
            Napier.e("Error checking if user $targetUserId is blocked", error)
        }
    }
}
