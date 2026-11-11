package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.BlockDTO
import com.synapse.social.studioasinc.shared.data.dto.BlockedUserIdDTO
import com.synapse.social.studioasinc.shared.data.dto.BlockWithUserDTO
import com.synapse.social.studioasinc.shared.data.dto.UserProfileDTO
import com.synapse.social.studioasinc.shared.domain.model.BlockStatus
import com.synapse.social.studioasinc.shared.domain.model.User
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
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
    private val usersTableName = "users"
    
    /**
     * Creates a new block record in Supabase.
     * Checks for existing block before creating to prevent duplicates.
     */
    suspend fun createBlock(targetUserId: String): Result<BlockDTO> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = currentUserId()
            
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
            val currentUserId = currentUserId()
            
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
     *
     * The profile lookup is a second, explicit query instead of a PostgREST embedding. The block
     * rows therefore load even when the embedding relation cannot be resolved, and each profile is
     * attached whenever it can be read - so a newly blocked account keeps its real name and avatar
     * in the list instead of degrading to a generic placeholder.
     */
    suspend fun getBlockedUsers(): Result<List<BlockWithUserDTO>> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = currentUserId()
            
            val blocks = client.postgrest[tableName]
                .select(columns = Columns.list("id", "blocker_id", "blocked_id", "created_at")) {
                    filter {
                        eq("blocker_id", currentUserId)
                    }
                }
                .decodeList<BlockDTO>()

            if (blocks.isEmpty()) return@runCatching emptyList()

            val profiles = loadProfiles(blocks.map { it.blockedId })

            blocks.map { block ->
                val profile = profiles[block.blockedId]
                BlockWithUserDTO(
                    id = block.id,
                    blockerId = block.blockerId,
                    blockedId = block.blockedId,
                    createdAt = block.createdAt,
                    blockedUser = profile?.let {
                        UserProfileDTO(
                            uid = it.uid,
                            username = it.username,
                            avatar = it.avatar,
                            displayName = it.displayName ?: it.name ?: it.nickname
                        )
                    }
                )
            }
        }.onFailure { error ->
            Napier.e("Error fetching blocked users", error)
        }
    }

    /**
     * Returns the ids of every account the current user has blocked.
     *
     * The messaging path needs the whole set at once so it can drop messages from those accounts
     * without a query per delivered message.
     */
    suspend fun getBlockedUserIds(): Result<Set<String>> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = currentUserId()
            
            client.postgrest[tableName]
                .select(columns = Columns.list("blocked_id")) {
                    filter {
                        eq("blocker_id", currentUserId)
                    }
                }
                .decodeList<BlockedUserIdDTO>()
                .map { it.blockedId }
                .filter { it.isNotBlank() }
                .toSet()
        }.onFailure { error ->
            Napier.e("Error fetching blocked user ids", error)
        }
    }
    
    /**
     * Checks if a specific user is blocked by the current user.
     */
    suspend fun isUserBlocked(targetUserId: String): Result<Boolean> = withContext(AppDispatchers.IO) {
        runCatching {
            blockExists(blockerId = currentUserId(), blockedId = targetUserId)
        }.onFailure { error ->
            Napier.e("Error checking if user $targetUserId is blocked", error)
        }
    }

    /**
     * Resolves the block relationship between the current user and [otherUserId].
     *
     * Both directions are checked because a message must not be delivered either way, and the two
     * sides of a block must not be shown the same copy. A direction the reader is not allowed to
     * see (for example the other account's block row under row-level security) simply reads as
     * "no block" - the authoritative server-side rule still applies.
     */
    suspend fun getBlockStatus(otherUserId: String): Result<BlockStatus> = withContext(AppDispatchers.IO) {
        runCatching {
            val currentUserId = currentUserId()
            if (otherUserId.isBlank() || otherUserId == currentUserId) {
                return@runCatching BlockStatus.NONE
            }

            // Each direction is read defensively: a row the reader is not allowed to see must
            // degrade to "no block" for that direction only, instead of failing the whole check and
            // leaving the conversation without a usable block state.
            val blockedByMe = runCatching { blockExists(currentUserId, otherUserId) }.getOrDefault(false)
            val blockedMe = runCatching { blockExists(otherUserId, currentUserId) }.getOrDefault(false)

            when {
                blockedByMe && blockedMe -> BlockStatus.BLOCKED_BOTH
                blockedByMe -> BlockStatus.BLOCKED_BY_ME
                blockedMe -> BlockStatus.BLOCKED_ME
                else -> BlockStatus.NONE
            }
        }.onFailure { error ->
            Napier.e("Error checking block status for user $otherUserId", error)
        }
    }

    /** True when [blockerId] has a block row against [blockedId]. */
    private suspend fun blockExists(blockerId: String, blockedId: String): Boolean {
        val count = client.postgrest[tableName]
            .select(columns = Columns.list("id")) {
                filter {
                    eq("blocker_id", blockerId)
                    eq("blocked_id", blockedId)
                }
                count(Count.EXACT)
            }
            .countOrNull() ?: 0

        return count > 0
    }

    /**
     * Loads the profiles behind a set of blocked ids.
     *
     * A profile that cannot be read is omitted rather than failing the caller; the row then keeps
     * whatever the block record itself carries.
     */
    private suspend fun loadProfiles(userIds: List<String>): Map<String, User> {
        val ids = userIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()

        return runCatching {
            client.postgrest[usersTableName]
                .select {
                    filter {
                        isIn("uid", ids)
                    }
                }
                .decodeList<User>()
        }.getOrElse { error ->
            Napier.w("Could not load profiles for ${ids.size} blocked account(s): ${error.message}")
            emptyList()
        }.associateBy { it.uid }
    }

    private fun currentUserId(): String = client.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("User not authenticated")
}
