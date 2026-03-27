package com.synapse.social.studioasinc.data.remote.services

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class SupabaseFollowService @Inject constructor() {

    private val client = SupabaseClient.client
    private val databaseService = SupabaseDatabaseService()



    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }


                val existingFollow = client.from("follows")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                    .decodeList<JsonObject>()

                if (existingFollow.isNotEmpty()) {

                    return@withContext Result.success(Unit)
                }


                val followData = mapOf(
                    "follower_id" to followerId,
                    "following_id" to followingId
                )

                val insertResult = databaseService.insert("follows", followData)

                insertResult.fold(
                    onSuccess = {

                        updateFollowerCounts(followerId, followingId, true)
                        return@withContext Result.success(Unit)
                    },
                    onFailure = { error ->
                        return@withContext Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to follow user", e)
                Result.failure(e)
            }
        }
    }



    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {

                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured"))
                }


                client.from("follows").delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }


                updateFollowerCounts(followerId, followingId, false)

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to unfollow user", e)
                Result.failure(e)
            }
        }
    }



    suspend fun isFollowing(followerId: String, followingId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {

                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(false)
                }

                val result = client.from("follows")
                    .select(columns = Columns.raw("id")) {
                        filter {
                            eq("follower_id", followerId)
                            eq("following_id", followingId)
                        }
                    }
                    .decodeList<JsonObject>()

                Result.success(result.isNotEmpty())
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to check follow status", e)
                Result.failure(e)
            }
        }
    }



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



    suspend fun getFollowStats(userId: String): Result<Map<String, Int>> {
        return withContext(Dispatchers.IO) {
            try {

                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(mapOf(
                        "followers_count" to 0,
                        "following_count" to 0
                    ))
                }


                val userResult = databaseService.selectWhere("users", "followers_count, following_count", "uid", userId)

                userResult.fold(
                    onSuccess = { users ->
                        val user = users.firstOrNull()
                        if (user != null) {
                            val stats = mapOf(
                                "followers_count" to (user["followers_count"]?.toString()?.toIntOrNull() ?: 0),
                                "following_count" to (user["following_count"]?.toString()?.toIntOrNull() ?: 0)
                            )
                            Result.success(stats)
                        } else {
                            Result.success(mapOf("followers_count" to 0, "following_count" to 0))
                        }
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get follow stats", e)
                Result.failure(e)
            }
        }
    }



    private suspend fun updateFollowerCounts(followerId: String, followingId: String, isFollow: Boolean) {
        try {

            val followerStats = getFollowStats(followerId).getOrNull()
            if (followerStats != null) {
                val currentFollowing = followerStats["following_count"] ?: 0
                val newFollowingCount = if (isFollow) currentFollowing + 1 else maxOf(0, currentFollowing - 1)

                databaseService.update(
                    "users",
                    mapOf("following_count" to newFollowingCount),
                    "uid",
                    followerId
                )
            }


            val followingStats = getFollowStats(followingId).getOrNull()
            if (followingStats != null) {
                val currentFollowers = followingStats["followers_count"] ?: 0
                val newFollowersCount = if (isFollow) currentFollowers + 1 else maxOf(0, currentFollowers - 1)

                databaseService.update(
                    "users",
                    mapOf("followers_count" to newFollowersCount),
                    "uid",
                    followingId
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseFollowService", "Failed to update follower counts", e)
        }
    }



    suspend fun getSuggestedUsers(userId: String, limit: Int = 20): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {

                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }


                val followingResult = client.from("follows")
                    .select(columns = Columns.raw("following_id")) {
                        filter { eq("follower_id", userId) }
                    }
                    .decodeList<JsonObject>()

                val followingIds = followingResult.map {
                    it["following_id"].toString().removeSurrounding("\"")
                }.toMutableList()


                followingIds.add(userId)



                val allUsersResult = client.from("users")
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify, followers_count")) {
                        limit(100)
                    }
                    .decodeList<JsonObject>()


                val suggestedResult = allUsersResult.filter { jsonObject ->
                    val uid = jsonObject["uid"].toString().removeSurrounding("\"")
                    !followingIds.contains(uid)
                }.take(limit)

                val suggested = suggestedResult.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }

                Result.success(suggested)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseFollowService", "Failed to get suggested users", e)
                Result.failure(e)
            }
        }
    }
}
