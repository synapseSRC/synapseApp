package com.synapse.social.studioasinc.data.source

import android.util.Log
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.domain.model.UserStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import javax.inject.Inject

class CommentRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TAG = "CommentRemoteDataSource"
    }

    suspend fun fetchComments(postId: String, limit: Int, offset: Int): List<CommentWithUser> = withContext(Dispatchers.IO) {
        val response = client.from("comments")
            .select(columns = Columns.raw("*, users(uid, username, display_name, avatar, bio, verify, status, account_type, followers_count, following_count, posts_count, banned)")) {
                filter {
                    eq("post_id", postId)
                    filterNot("is_deleted", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, true)
                    // Only fetch top-level comments
                    filter("parent_comment_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS, "null")
                }
                order("created_at", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }

        response.decodeList<JsonObject>().mapNotNull { parseCommentFromJson(it) }
    }

    suspend fun getComment(commentId: String): CommentWithUser? = withContext(Dispatchers.IO) {
        val response = client.from("comments")
            .select(
                columns = Columns.raw("""
                    *,
                    users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                """.trimIndent())
            ) {
                filter { eq("id", commentId) }
            }
            .decodeSingleOrNull<JsonObject>()

        response?.let { parseCommentFromJson(it) }
    }

    suspend fun fetchReplies(parentCommentId: String, limit: Int, offset: Int, ascending: Boolean = true): List<CommentWithUser> = withContext(Dispatchers.IO) {
        val response = client.from("comments")
            .select(
                columns = Columns.raw("""
                    *,
                    users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                """.trimIndent())
            ) {
                filter {
                    eq("parent_comment_id", parentCommentId)
                    filterNot("is_deleted", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, true)
                }
                order("created_at", if (ascending) Order.ASCENDING else Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<JsonObject>()

        response.mapNotNull { parseCommentFromJson(it) }
    }

    suspend fun fetchAllReplies(parentCommentId: String): List<CommentWithUser> = withContext(Dispatchers.IO) {
        val response = client.from("comments")
            .select(
                columns = Columns.raw("""
                    *,
                    users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                """.trimIndent())
            ) {
                filter { eq("parent_comment_id", parentCommentId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<JsonObject>()

        response.mapNotNull { parseCommentFromJson(it) }
    }

    suspend fun fetchUserComments(userId: String, limit: Int, offset: Int): List<CommentWithUser> = withContext(Dispatchers.IO) {
        val response = client.from("comments")
            .select(columns = Columns.raw("*, users(uid, username, display_name, avatar, bio, verify, status, account_type, followers_count, following_count, posts_count, banned)")) {
                filter {
                    eq("user_id", userId)
                    filterNot("is_deleted", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, true)
                }
                order("created_at", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }

        response.decodeList<JsonObject>().mapNotNull { parseCommentFromJson(it) }
    }

    suspend fun addComment(
        id: String,
        postId: String,
        userId: String,
        content: String,
        mediaUrl: String?,
        parentCommentId: String?
    ): CommentWithUser? = withContext(Dispatchers.IO) {
        val insertData = buildJsonObject {
            put("id", id)
            put("post_id", postId)
            put("user_id", userId)
            put("content", content)
            if (mediaUrl != null) put("media_url", mediaUrl)
            if (parentCommentId != null) put("parent_comment_id", parentCommentId)
            put("created_at", java.time.Instant.now().toString())
            put("updated_at", java.time.Instant.now().toString())
        }

        val response = client.from("comments")
            .insert(insertData) {
                select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                )
            }
            .decodeSingleOrNull<JsonObject>()

        response?.let { parseCommentFromJson(it) }
    }

    suspend fun updateComment(commentId: String, newContent: String): CommentWithUser? = withContext(Dispatchers.IO) {
        val response = client.from("comments")
            .update({
                set("content", newContent)
                set("is_edited", true)
                set("edited_at", java.time.Instant.now().toString())
            }) {
                filter { eq("id", commentId) }
                select(Columns.raw("*, users(uid, username, display_name, avatar, verify)"))
            }
            .decodeSingleOrNull<JsonObject>()

        response?.let { parseCommentFromJson(it) }
    }

    suspend fun markCommentDeleted(commentId: String) = withContext(Dispatchers.IO) {
        client.from("comments")
            .update({ set("is_deleted", true) }) {
                filter { eq("id", commentId) }
            }
    }

    suspend fun pinComment(commentId: String) = withContext(Dispatchers.IO) {
        client.from("comments")
            .update({ set("is_pinned", true) }) {
                filter { eq("id", commentId) }
            }
    }

    suspend fun hideComment(commentId: String, currentUserId: String) = withContext(Dispatchers.IO) {
        client.from("comments")
            .update({
                set("is_hidden", true)
                set("hidden_by", currentUserId)
            }) {
                filter { eq("id", commentId) }
            }
    }

    suspend fun reportComment(commentId: String, reporterId: String, reason: String) = withContext(Dispatchers.IO) {
        client.from("comment_reports")
            .insert(buildJsonObject {
                put("comment_id", commentId)
                put("reporter_id", reporterId)
                put("reason", reason)
                put("created_at", java.time.Instant.now().toString())
            })
    }

    suspend fun updateRepliesCount(commentId: String, delta: Int) = withContext(Dispatchers.IO) {
        try {
            client.postgrest.rpc(
                "increment_replies_count",
                mapOf("comment_id" to commentId, "delta" to delta)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update replies count: ${e.message}")
        }
    }

    suspend fun updatePostCommentsCount(postId: String, delta: Int) = withContext(Dispatchers.IO) {
        try {
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()

            val currentCount = post?.get("comments_count")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0
            val newCount = maxOf(0, currentCount + delta)

            client.from("posts")
                .update({ set("comments_count", newCount) }) {
                    filter { eq("id", postId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update post comments count: ${e.message}")
        }
    }

    suspend fun getPostAuthorId(postId: String): String? = withContext(Dispatchers.IO) {
        val post = client.from("posts")
            .select { filter { eq("id", postId) } }
            .decodeSingleOrNull<JsonObject>()
        post?.get("author_uid")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
    }

    suspend fun getCurrentUser() = withContext(Dispatchers.IO) {
        client.auth.currentUserOrNull()
    }

    private fun parseCommentFromJson(data: JsonObject): CommentWithUser? {
        return try {
            val user = parseUserProfileFromJson(data["users"]?.jsonObject)
            val commentId = data["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null

            CommentWithUser(
                id = commentId,
                postId = data["post_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null,
                userId = data["user_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null,
                parentCommentId = data["parent_comment_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                content = data["content"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                mediaUrl = data["media_url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                createdAt = data["created_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                updatedAt = data["updated_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                likesCount = data["likes_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                repliesCount = data["replies_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                isDeleted = data["is_deleted"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false,
                isEdited = data["is_edited"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false,
                isPinned = data["is_pinned"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false,
                user = user,
                reactionSummary = emptyMap(),
                userReaction = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse comment: ${e.message}")
            null
        }
    }

    private fun parseUserProfileFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null

        return try {
            val avatarPath = userData["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
            val avatarUrl = avatarPath?.let { path ->
                if (path.startsWith("http")) path else com.synapse.social.studioasinc.shared.core.network.SupabaseClient.constructAvatarUrl(path)
            }

            UserProfile(
                uid = userData["uid"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return null,
                username = userData["username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                displayName = userData["display_name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                email = "",
                bio = userData["bio"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull,
                avatar = avatarUrl,
                followersCount = userData["followers_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                followingCount = userData["following_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                postsCount = userData["posts_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0,
                status = UserStatus.fromString(userData["status"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull),
                account_type = userData["account_type"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "user",
                verify = userData["verify"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false,
                banned = userData["banned"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user profile: ${e.message}")
            null
        }
    }
}
