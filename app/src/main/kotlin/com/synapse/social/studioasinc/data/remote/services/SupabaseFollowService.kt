package com.synapse.social.studioasinc.data.remote.services

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseFollowService @Inject constructor() {
    private val client = SupabaseClient.client

    @Suppress("UNUSED_PARAMETER")
    suspend fun followUser(followerId: String, followingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) return@withContext Result.failure(Exception("Supabase not configured"))
            if (client.auth.currentUserOrNull() == null) return@withContext Result.failure(Exception("User not authenticated"))
            android.util.Log.d("SupabaseFollowService", "Attempting to follow: followerId=$followerId, followingId=$followingId")
            val response = client.postgrest.rpc("request_follow", buildJsonObject { put("p_target_uid", followingId) }).decodeSingle<JsonElement>()
            android.util.Log.d("SupabaseFollowService", "Follow request successful: $response")
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseFollowService", "Failed to request follow: ${e.message}", e)
            android.util.Log.e("SupabaseFollowService", "Error details: ${e::class.simpleName} - ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) return@withContext Result.failure(Exception("Supabase not configured"))
            if (client.auth.currentUserOrNull() == null) return@withContext Result.failure(Exception("User not authenticated"))
            client.postgrest.rpc("unfollow_user", buildJsonObject { put("p_target_uid", followingId) }).decodeSingle<JsonElement>()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("SupabaseFollowService", "Failed to unfollow user", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowStatus(targetUid: String): Result<ProfileFollowStatus> = withContext(Dispatchers.IO) {
        try {
            if (client.auth.currentUserOrNull() == null) return@withContext Result.success(ProfileFollowStatus.NONE)
            val status = client.postgrest.rpc("get_follow_status", buildJsonObject { put("p_target_uid", targetUid) }).decodeSingle<String>()
            Result.success(when (status) {
                "following" -> ProfileFollowStatus.FOLLOWING
                "requested" -> ProfileFollowStatus.REQUESTED
                else -> ProfileFollowStatus.NONE
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isPrivateProfile(targetUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val profile = client.from("public_profiles").select(columns = Columns.raw("is_private")) {
                filter { eq("uid", targetUid) }
            }.decodeSingleOrNull<JsonObject>() ?: return@withContext Result.failure(Exception("Profile not found"))
            Result.success(profile["is_private"]?.jsonPrimitive?.booleanOrNull == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> =
        getFollowStatus(followingId).map { it == ProfileFollowStatus.FOLLOWING }

    suspend fun getFollowers(userId: String, limit: Int = 50): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }

                val followsResult = client.from("follows")
                    .select(columns = Columns.raw("follower_id")) {
                        filter { eq("following_id", userId) }
                        limit(limit.toLong())
                    }
                    .decodeList<JsonObject>()

                val followerIds = followsResult.map {
                    it["follower_id"].toString().removeSurrounding("\"")
                }

                if (followerIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val usersResult = client.from("users")
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify")) {
                        filter { isIn("uid", followerIds) }
                    }
                    .decodeList<JsonObject>()

                val followers = usersResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }

                Result.success(followers)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get followers", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getFollowing(userId: String, limit: Int = 50): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }

                val followsResult = client.from("follows")
                    .select(columns = Columns.raw("following_id")) {
                        filter { eq("follower_id", userId) }
                        limit(limit.toLong())
                    }
                    .decodeList<JsonObject>()

                val followingIds = followsResult.map {
                    it["following_id"].toString().removeSurrounding("\"")
                }

                if (followingIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val usersResult = client.from("users")
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify")) {
                        filter { isIn("uid", followingIds) }
                    }
                    .decodeList<JsonObject>()

                val following = usersResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }

                Result.success(following)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get following", e)
                Result.failure(e)
            }
        }
    }
}
