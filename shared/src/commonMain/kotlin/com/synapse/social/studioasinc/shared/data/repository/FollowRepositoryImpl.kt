package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.FollowRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class FollowRepositoryImpl(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : FollowRepository {

    override suspend fun getFollowers(userId: String): Result<List<User>> {
        return withContext(Dispatchers.Default) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }

                val followsResult = client.postgrest["follows"]
                    .select(columns = Columns.raw("follower_id")) {
                        filter { eq("following_id", userId) }
                        limit(50)
                    }
                    .decodeList<JsonObject>()

                val followerIds = followsResult.map {
                    it["follower_id"]?.jsonPrimitive?.content ?: ""
                }.filter { it.isNotEmpty() }

                if (followerIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val usersResult = client.postgrest["users"]
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify")) {
                        filter { isIn("uid", followerIds) }
                    }
                    .decodeList<JsonObject>()

                val followers = usersResult.map { jsonObject ->
                    mapToUser(jsonObject)
                }

                Result.success(followers)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getFollowing(userId: String): Result<List<User>> {
        return withContext(Dispatchers.Default) {
            try {
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.success(emptyList())
                }

                val followsResult = client.postgrest["follows"]
                    .select(columns = Columns.raw("following_id")) {
                        filter { eq("follower_id", userId) }
                        limit(50)
                    }
                    .decodeList<JsonObject>()

                val followingIds = followsResult.map {
                    it["following_id"]?.jsonPrimitive?.content ?: ""
                }.filter { it.isNotEmpty() }

                if (followingIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val usersResult = client.postgrest["users"]
                    .select(columns = Columns.raw("uid, username, display_name, avatar, verify")) {
                        filter { isIn("uid", followingIds) }
                    }
                    .decodeList<JsonObject>()

                val following = usersResult.map { jsonObject ->
                    mapToUser(jsonObject)
                }

                Result.success(following)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun mapToUser(jsonObject: JsonObject): User {
        val uid = jsonObject["uid"]?.jsonPrimitive?.content ?: ""
        val username = jsonObject["username"]?.jsonPrimitive?.content ?: ""
        val displayName = jsonObject["display_name"]?.jsonPrimitive?.content
        val avatarPath = jsonObject["avatar"]?.jsonPrimitive?.content
        val verify = jsonObject["verify"]?.jsonPrimitive?.content?.toBoolean() ?: false

        val avatarUrl = avatarPath?.let { constructAvatarUrl(it) }

        return User(
            uid = uid,
            username = username,
            displayName = displayName,
            avatar = avatarUrl,
            verify = verify
        )
    }

    private fun constructAvatarUrl(path: String): String {
        if (path.startsWith("http")) return path
        val baseUrl = SynapseConfig.SUPABASE_URL
        val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        return "$cleanBaseUrl/storage/v1/object/public/avatars/$path"
    }
}
