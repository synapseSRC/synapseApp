package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

internal data class ProfileData(
    val username: String?,
    val avatarUrl: String?,
    val isVerified: Boolean
)

internal data class CacheEntry<T>(val data: T, val timestamp: Long = System.currentTimeMillis()) {
    fun isExpired() = System.currentTimeMillis() - timestamp > PostRepositoryUtils.CACHE_EXPIRATION_MS
}

internal class PostRepositoryUtils(private val client: JanSupabaseClient) {

    val profileCache = ConcurrentHashMap<String, CacheEntry<ProfileData>>()
    val userProfileCache = ConcurrentHashMap<String, Result<ProfileData>>()

    companion object {
        const val PREF_LAST_SYNC_TIME = "last_post_sync_time"
        const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
        const val TAG = "PostRepository"
        val PGRST_REGEX = Regex("PGRST\\d+")
        val COLUMN_REGEX = Regex("column \"([^\"]+)\"")

        fun constructMediaUrl(storagePath: String): String {
            return SupabaseClient.constructMediaUrl(storagePath)
        }

        fun constructAvatarUrl(storagePath: String): String {
            return SupabaseClient.constructAvatarUrl(storagePath)
        }

        fun extractColumnInfo(message: String): String {
            val columnMatch = COLUMN_REGEX.find(message)
            return columnMatch?.groupValues?.get(1) ?: "unknown column"
        }

        fun mapSupabaseError(exception: Exception): String {
            val message = exception.message ?: "Unknown error"
            val pgrstMatch = PGRST_REGEX.find(message)
            if (pgrstMatch != null) {
                android.util.Log.e(TAG, "Supabase PostgREST error code: ${pgrstMatch.value}")
            }
            android.util.Log.e(TAG, "Supabase error: $message", exception)

            return when {
                message.contains("PGRST200") -> "Relation/table not found in schema"
                message.contains("PGRST100") -> "Database column mismatch: ${extractColumnInfo(message)}"
                message.contains("PGRST116") -> "No rows returned (expected single)"
                message.contains("relation", ignoreCase = true) -> "Database table does not exist"
                message.contains("column", ignoreCase = true) -> "Database column mismatch: ${extractColumnInfo(message)}"
                message.contains("does not exist", ignoreCase = true) -> "Database column mismatch: ${extractColumnInfo(message)}"
                message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) ->
                    "Permission denied. Row-level security policy blocked this operation."
                message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) ->
                    "Connection failed. Please check your internet connection."
                message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
                message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
                message.contains("serialization", ignoreCase = true) -> "Data format error."
                else -> "Database error: $message"
            }
        }
    }

    suspend fun fetchUserProfile(userId: String): ProfileData? {
        profileCache[userId]?.let { entry ->
            if (!entry.isExpired()) return entry.data
        }

        return try {
            val user = client.from("users").select {
                filter { eq("uid", userId) }
            }.decodeSingleOrNull<JsonObject>()

            if (user != null) {
                val dName = user["display_name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                val profile = ProfileData(
                    username = if (!dName.isNullOrBlank()) dName else user["username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    avatarUrl = user["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user["verify"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false
                )
                profileCache[userId] = CacheEntry(profile)
                profile
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user profile for $userId", e)
            null
        }
    }

    suspend fun fetchUserProfilesBatch(userIds: List<String>) {
        try {
            val users = client.from("users").select {
                filter { isIn("uid", userIds) }
            }.decodeList<JsonObject>()

            users.forEach { user ->
                val uid = user["uid"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return@forEach
                val dName = user["display_name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                val profile = ProfileData(
                    username = if (!dName.isNullOrBlank()) dName else user["username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                    avatarUrl = user["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user["verify"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false
                )
                profileCache[uid] = CacheEntry(profile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to batch fetch user profiles", e)
        }
    }
}
