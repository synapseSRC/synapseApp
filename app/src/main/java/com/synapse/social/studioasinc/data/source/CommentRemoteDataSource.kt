package com.synapse.social.studioasinc.data.source

import android.util.Log
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.domain.model.UserStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import javax.inject.Inject

/**
 * X-style threaded posts: "comments" are posts with in_reply_to_post_id set.
 * - Top-level replies to a post: in_reply_to_post_id = postId
 * - Nested replies to a comment: in_reply_to_post_id = commentId
 * - root_post_id always points to the original root post of the thread.
 */
class CommentRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TAG = "CommentRemoteDataSource"
        private const val POST_COLUMNS = """
            id, author_uid, post_text, post_image, media_items,
            in_reply_to_post_id, root_post_id,
            likes_count, comments_count, views_count,
            is_deleted, is_edited, edited_at, created_at, updated_at,
            users!posts_author_uid_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
        """
    }

    suspend fun fetchComments(postId: String, limit: Int, offset: Int): List<CommentWithUser> = withContext(Dispatchers.IO) {
        client.from("posts")
            .select(Columns.raw(POST_COLUMNS)) {
                filter {
                    eq("in_reply_to_post_id", postId)
                    eq("is_deleted", false)
                }
                order("created_at", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<JsonObject>()
            .mapNotNull { parsePostAsComment(it) }
    }

    suspend fun getComment(commentId: String): CommentWithUser? = withContext(Dispatchers.IO) {
        client.from("posts")
            .select(Columns.raw(POST_COLUMNS)) {
                filter { eq("id", commentId) }
            }
            .decodeSingleOrNull<JsonObject>()
            ?.let { parsePostAsComment(it) }
    }

    suspend fun fetchReplies(parentCommentId: String, limit: Int, offset: Int, ascending: Boolean = true): List<CommentWithUser> = withContext(Dispatchers.IO) {
        client.from("posts")
            .select(Columns.raw(POST_COLUMNS)) {
                filter {
                    eq("in_reply_to_post_id", parentCommentId)
                    eq("is_deleted", false)
                }
                order("created_at", if (ascending) Order.ASCENDING else Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<JsonObject>()
            .mapNotNull { parsePostAsComment(it) }
    }

    suspend fun fetchAllReplies(parentCommentId: String): List<CommentWithUser> = withContext(Dispatchers.IO) {
        client.from("posts")
            .select(Columns.raw(POST_COLUMNS)) {
                filter {
                    eq("in_reply_to_post_id", parentCommentId)
                    eq("is_deleted", false)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<JsonObject>()
            .mapNotNull { parsePostAsComment(it) }
    }

    suspend fun fetchUserComments(userId: String, limit: Int, offset: Int): List<CommentWithUser> = withContext(Dispatchers.IO) {
        client.from("posts")
            .select(Columns.raw(POST_COLUMNS)) {
                filter {
                    eq("author_uid", userId)
                    filter("in_reply_to_post_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.IS_NOT, "null")
                    eq("is_deleted", false)
                }
                order("created_at", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<JsonObject>()
            .mapNotNull { parsePostAsComment(it) }
    }

    suspend fun addComment(
        id: String,
        postId: String,
        userId: String,
        content: String,
        mediaUrl: String?,
        parentCommentId: String?
    ): CommentWithUser? = withContext(Dispatchers.IO) {
        // In X-style threading: root_post_id is always the original post,
        // in_reply_to_post_id is the direct parent (post or comment).
        val rootPostId = if (parentCommentId != null) {
            // Fetch parent's root_post_id to propagate the thread root
            getComment(parentCommentId)?.postId ?: postId
        } else {
            postId
        }

        val insertData = buildJsonObject {
            put("id", id)
            put("author_uid", userId)
            put("post_text", content)
            if (mediaUrl != null) put("post_image", mediaUrl)
            put("in_reply_to_post_id", parentCommentId ?: postId)
            put("root_post_id", rootPostId)
            put("post_type", "TEXT")
            put("created_at", java.time.Instant.now().toString())
            put("updated_at", java.time.Instant.now().toString())
        }

        client.from("posts")
            .insert(insertData) {
                select(Columns.raw(POST_COLUMNS))
            }
            .decodeSingleOrNull<JsonObject>()
            ?.let { parsePostAsComment(it) }
    }

    suspend fun updateComment(commentId: String, newContent: String): CommentWithUser? = withContext(Dispatchers.IO) {
        client.from("posts")
            .update({
                set("post_text", newContent)
                set("is_edited", true)
                set("edited_at", java.time.Instant.now().toString())
                set("updated_at", java.time.Instant.now().toString())
            }) {
                filter { eq("id", commentId) }
                select(Columns.raw(POST_COLUMNS))
            }
            .decodeSingleOrNull<JsonObject>()
            ?.let { parsePostAsComment(it) }
    }

    suspend fun markCommentDeleted(commentId: String) = withContext(Dispatchers.IO) {
        client.from("posts")
            .update({
                set("is_deleted", true)
                set("deleted_at", java.time.Instant.now().toString())
            }) {
                filter { eq("id", commentId) }
            }
    }

    /** Posts don't have is_pinned — no-op for now. */
    suspend fun pinComment(commentId: String) = Unit

    /** Posts don't have is_hidden — no-op for now. */
    suspend fun hideComment(commentId: String, currentUserId: String) = Unit

    suspend fun reportComment(commentId: String, reporterId: String, reason: String) = withContext(Dispatchers.IO) {
        client.from("reports")
            .insert(buildJsonObject {
                put("reporter_id", reporterId)
                put("target_id", commentId)
                put("target_type", "comment")
                put("reason", reason)
                put("created_at", java.time.Instant.now().toString())
            })
    }

    suspend fun updateRepliesCount(commentId: String, delta: Int) = withContext(Dispatchers.IO) {
        try {
            val post = client.from("posts")
                .select(Columns.raw("id, comments_count")) { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
            val current = post?.get("comments_count")?.jsonPrimitive?.intOrNull ?: 0
            client.from("posts")
                .update({ set("comments_count", maxOf(0, current + delta)) }) {
                    filter { eq("id", commentId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update replies count: ${e.message}")
        }
    }

    suspend fun updatePostCommentsCount(postId: String, delta: Int) = withContext(Dispatchers.IO) {
        try {
            val post = client.from("posts")
                .select(Columns.raw("id, comments_count")) { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
            val current = post?.get("comments_count")?.jsonPrimitive?.intOrNull ?: 0
            client.from("posts")
                .update({ set("comments_count", maxOf(0, current + delta)) }) {
                    filter { eq("id", postId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update post comments count: ${e.message}")
        }
    }

    suspend fun getPostAuthorId(postId: String): String? = withContext(Dispatchers.IO) {
        client.from("posts")
            .select(Columns.raw("author_uid")) { filter { eq("id", postId) } }
            .decodeSingleOrNull<JsonObject>()
            ?.get("author_uid")?.jsonPrimitive?.contentOrNull
    }

    suspend fun getCurrentUser() = withContext(Dispatchers.IO) {
        client.auth.currentUserOrNull()
    }

    private fun parsePostAsComment(data: JsonObject): CommentWithUser? {
        return try {
            val id = data["id"]?.jsonPrimitive?.contentOrNull ?: return null
            val authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: return null
            // postId = root_post_id (the thread root), falls back to in_reply_to_post_id
            val postId = data["root_post_id"]?.jsonPrimitive?.contentOrNull
                ?: data["in_reply_to_post_id"]?.jsonPrimitive?.contentOrNull
                ?: return null
            val parentCommentId = data["in_reply_to_post_id"]?.jsonPrimitive?.contentOrNull

            CommentWithUser(
                id = id,
                postId = postId,
                userId = authorUid,
                parentCommentId = if (parentCommentId != postId) parentCommentId else null,
                content = data["post_text"]?.jsonPrimitive?.contentOrNull ?: "",
                mediaUrl = data["post_image"]?.jsonPrimitive?.contentOrNull,
                createdAt = data["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                updatedAt = data["updated_at"]?.jsonPrimitive?.contentOrNull,
                likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
                repliesCount = data["comments_count"]?.jsonPrimitive?.intOrNull ?: 0,
                isDeleted = data["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false,
                isEdited = data["is_edited"]?.jsonPrimitive?.booleanOrNull ?: false,
                isPinned = false,
                user = parseUserFromJson(data["users"]?.jsonObject),
                reactionSummary = emptyMap(),
                userReaction = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse post as comment: ${e.message}")
            null
        }
    }

    private fun parseUserFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null
        return try {
            val avatarPath = userData["avatar"]?.jsonPrimitive?.contentOrNull
            val avatarUrl = avatarPath?.let {
                if (it.startsWith("http")) it
                else com.synapse.social.studioasinc.shared.core.network.SupabaseClient.constructAvatarUrl(it)
            }
            UserProfile(
                uid = userData["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                username = userData["username"]?.jsonPrimitive?.contentOrNull ?: "",
                displayName = userData["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                email = "",
                bio = userData["bio"]?.jsonPrimitive?.contentOrNull,
                avatar = avatarUrl,
                followersCount = userData["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = userData["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                postsCount = userData["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
                status = UserStatus.fromString(userData["status"]?.jsonPrimitive?.contentOrNull),
                account_type = userData["account_type"]?.jsonPrimitive?.contentOrNull ?: "user",
                verify = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false,
                banned = userData["banned"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user: ${e.message}")
            null
        }
    }
}
