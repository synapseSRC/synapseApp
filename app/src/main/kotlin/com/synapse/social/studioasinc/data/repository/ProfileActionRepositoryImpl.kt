package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import com.synapse.social.studioasinc.domain.model.ProfileFollowRequest
import com.synapse.social.studioasinc.domain.repository.ProfilePrivacyRepository
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileActionRepositoryImpl @Inject constructor() : ProfilePrivacyRepository {
    private val supabase = SupabaseClient.client

    @Suppress("UNUSED_PARAMETER")
    override suspend fun lockProfile(userId: String, isLocked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (supabase.auth.currentUserOrNull() == null) {
                return@withContext Result.failure(IllegalStateException("User not authenticated"))
            }
            val updated = supabase.postgrest.rpc("set_profile_privacy", buildJsonObject {
                put("p_is_private", isLocked)
            }).decodeSingle<Boolean>()
            privacyUpdateResult(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowStatus(targetUid: String): Result<ProfileFollowStatus> = withContext(Dispatchers.IO) {
        try {
            if (supabase.auth.currentUserOrNull() == null) return@withContext Result.success(ProfileFollowStatus.NONE)
            val status = supabase.postgrest.rpc("get_follow_status", buildJsonObject {
                put("p_target_uid", targetUid)
            }).decodeSingle<String>()
            Result.success(when (status) {
                "following" -> ProfileFollowStatus.FOLLOWING
                "requested" -> ProfileFollowStatus.REQUESTED
                else -> ProfileFollowStatus.NONE
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingFollowRequests(): Result<List<ProfileFollowRequest>> = withContext(Dispatchers.IO) {
        try {
            if (supabase.auth.currentUserOrNull() == null) return@withContext Result.failure(IllegalStateException("User not authenticated"))
            val requests = supabase.postgrest.rpc("get_pending_follow_requests")
                .decodeList<PendingFollowRequestDto>()
                .map { row ->
                    ProfileFollowRequest(
                        requesterId = row.requesterId,
                        username = row.username.orEmpty(),
                        displayName = row.displayName,
                        avatar = row.avatar,
                        requestedAt = row.requestedAt
                    )
                }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToFollowRequest(requesterUid: String, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (supabase.auth.currentUserOrNull() == null) return@withContext Result.failure(IllegalStateException("User not authenticated"))
            val function = if (accept) "accept_follow_request" else "reject_follow_request"
            val changed = supabase.postgrest.rpc(function, buildJsonObject {
                put("p_requester_uid", requesterUid)
            }).decodeSingle<Boolean>()
            if (!changed) Result.failure(IllegalStateException("Follow request was not updated")) else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    private data class PendingFollowRequestDto(
        @SerialName("requester_id") val requesterId: String,
        val username: String? = null,
        @SerialName("display_name") val displayName: String? = null,
        val avatar: String? = null,
        @SerialName("requested_at") val requestedAt: String? = null
    )

    suspend fun archiveProfile(userId: String, isArchived: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("users")
                .update(mapOf("is_archived" to isArchived)) {
                    filter { eq("id", userId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(userId: String, blockedUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("blocked_users")
                .insert(mapOf(
                    "user_id" to userId,
                    "blocked_user_id" to blockedUserId
                ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportUser(userId: String, reportedUserId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("user_reports")
                .insert(mapOf(
                    "reporter_id" to userId,
                    "reported_user_id" to reportedUserId,
                    "reason" to reason
                ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun muteUser(userId: String, mutedUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("muted_users")
                .insert(mapOf(
                    "user_id" to userId,
                    "muted_user_id" to mutedUserId
                ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


internal fun privacyUpdateResult(updated: Boolean): Result<Unit> =
    if (updated) Result.success(Unit)
    else Result.failure(IllegalStateException("Profile privacy update affected no row"))
