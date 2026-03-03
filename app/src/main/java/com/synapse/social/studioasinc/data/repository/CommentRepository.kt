package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.local.database.CommentDao
import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import com.synapse.social.studioasinc.data.repository.CommentMapper
import com.synapse.social.studioasinc.domain.model.*
import com.synapse.social.studioasinc.domain.model.UserStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CommentRepository constructor(
    private val storageDatabase: StorageDatabase,
    private val client: SupabaseClient,
    private val commentDao: CommentDao,
    private val userRepository: UserRepository,
    @Named("ApplicationScope") private val externalScope: CoroutineScope,
    private val reactionRepository: ReactionRepository
) {
    private val TAG = "CommentRepository"

    companion object {
        private const val TAG = "CommentRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }

    fun getComments(postId: String): Flow<Result<List<Comment>>> {
        return flow {
            try {
                val comments = storageDatabase.commentQueries.selectByPostId(postId).executeAsList()
                emit(Result.success(comments.map { CommentMapper.toModel(it) }))
            } catch (e: Exception) {
                emit(Result.failure(Exception("Error getting comments from database: ${e.message}")))
            }
        }
    }

    suspend fun fetchComments(postId: String, limit: Int = 50, offset: Int = 0): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching comments for post: $postId")
            val response = client.from("comments")
                .select(columns = Columns.raw("*, users(uid, username, display_name, avatar, bio, verify, status, account_type, followers_count, following_count, posts_count, banned)")) {
                    filter {
                        eq("post_id", postId)
                    }
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }

            val comments = mutableListOf<CommentWithUser>()
            for (json in response.decodeList<JsonObject>()) {
                parseCommentFromJson(json)?.let {
                    comments.add(it)
                }
            }

            val populatedComments = reactionRepository.populateCommentReactions(comments)

            // Cache in DB
            val commentsToCache = populatedComments.map {
                CommentMapper.toSharedEntity(it.toComment(), it.user?.username, it.user?.avatar)
            }
            commentDao.insertAll(commentsToCache)

            Result.success(populatedComments)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comments: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun refreshComments(postId: String, limit: Int = 50, offset: Int = 0): Result<Unit> {
        return fetchComments(postId, limit, offset).map { Unit }
    }

    suspend fun getReplies(commentId: String): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching replies for comment: $commentId")
            val response = client.from("comments")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter { eq("parent_comment_id", commentId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<JsonObject>()

            Log.d(TAG, "Raw response size: ${response.size}")

            val replies = mutableListOf<CommentWithUser>()
            for (json in response) {
                parseCommentFromJson(json)?.let {
                    replies.add(it)
                }
            }

            // Optimized: Bulk fetch reactions
            val populatedReplies = reactionRepository.populateCommentReactions(replies)

            Log.d(TAG, "Successfully parsed ${populatedReplies.size} replies")

            val commentsToCache = populatedReplies.map {
                CommentMapper.toSharedEntity(it.toComment(), it.user?.username, it.user?.avatar)
            }
            commentDao.insertAll(commentsToCache)

            Result.success(populatedReplies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch paged comments: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentWithUser>> {
        return commentDao.getCommentsForPost(postId).map { entities ->
            entities.map { entity ->

                val comment = CommentMapper.toModel(entity)

                val user = try {
                    userRepository.getUserById(entity.authorUid).getOrNull()?.let { domainUser ->
                        UserProfile(
                            uid = domainUser.uid,
                            username = domainUser.username ?: "",
                            displayName = domainUser.displayName ?: "",
                            email = domainUser.email ?: "",
                            bio = domainUser.bio,
                            avatar = domainUser.avatar,
                            followersCount = domainUser.followersCount,
                            followingCount = domainUser.followingCount,
                            postsCount = domainUser.postsCount,
                            status = domainUser.status,
                            account_type = domainUser.accountType,
                            verify = domainUser.verify,
                            banned = domainUser.banned
                        )
                    }
                } catch (e: Exception) {
                    null
                }

                CommentWithUser(
                    id = comment.key,
                    postId = comment.postKey,
                    userId = comment.uid,
                    parentCommentId = comment.replyCommentKey,
                    content = comment.comment,
                    mediaUrl = null,
                    createdAt = comment.push_time,
                    updatedAt = null,
                    likesCount = 0,
                    repliesCount = 0,
                    isDeleted = false,
                    isEdited = false,
                    isPinned = false,
                    user = user,
                    reactionSummary = emptyMap(),
                    userReaction = null
                )
            }
        }
    }

    suspend fun createComment(postId: String, content: String, mediaUrl: String? = null, parentId: String? = null): Result<CommentWithUser> {
        return addComment(postId, content, parentId)
    }

    suspend fun addComment(postId: String, content: String, parentCommentId: String? = null): Result<CommentWithUser> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            if (content.isBlank()) {
                return@withContext Result.failure(Exception("Comment cannot be empty"))
            }

            val userId = currentUser.id
            val clientGeneratedId = java.util.UUID.randomUUID().toString()
            Log.d(TAG, "Creating comment for post: $postId by user: $userId with client-generated ID: $clientGeneratedId")

            var lastException: Exception? = null
            var resultComment: CommentWithUser? = null

            repeat(MAX_RETRIES) { attempt ->
                if (resultComment != null) return@repeat

                try {
                    val insertData = buildJsonObject {
                        put("id", clientGeneratedId)
                        put("post_id", postId)
                        put("user_id", userId)
                        put("content", content)
                        if (parentCommentId != null) put("parent_comment_id", parentCommentId)
                        put("created_at", java.time.Instant.now().toString())
                        put("updated_at", java.time.Instant.now().toString())
                    }

                    val response = try {
                        client.from("comments")
                            .insert(insertData) {
                                select(
                                    columns = Columns.raw("""
                                        *,
                                        users!comments_user_id_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                                    """.trimIndent())
                                )
                            }
                            .decodeSingleOrNull<JsonObject>()
                    } catch (e: RestException) {
                         throw e
                    }

                    if (response == null) {
                        return@withContext Result.failure(Exception("Failed to create or fetch comment"))
                    }

                    val comment = parseCommentFromJson(response)
                        ?: return@withContext Result.failure(Exception("Failed to parse created comment"))

                    resultComment = comment

                    // Cache in DB
                    val commentEntity = CommentMapper.toSharedEntity(comment.toComment(), comment.user?.username, comment.user?.avatar)
                    commentDao.insertAll(listOf(commentEntity))

                    externalScope.launch {
                        if (parentCommentId != null) {
                            updateRepliesCount(parentCommentId, 1)
                        }
                    }
                    externalScope.launch {
                        processMentions(postId, comment.id, content, userId, parentCommentId)
                    }
                    externalScope.launch {
                         updatePostCommentsCount(postId, 1)
                    }

                    Log.d(TAG, "Comment created successfully: ${comment.id}")
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == MAX_RETRIES - 1) throw e
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }

            if (resultComment != null) {
                Result.success(resultComment!!)
            } else {
                 Result.failure(lastException ?: Exception("Failed to add comment"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))


            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
                ?: return@withContext Result.failure(Exception("Comment not found"))

            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {

                val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
                if (postId != null) {
                    val post = client.from("posts")
                        .select { filter { eq("id", postId) } }
                        .decodeSingleOrNull<JsonObject>()
                    val postAuthor = post?.get("author_uid")?.jsonPrimitive?.contentOrNull

                    if (postAuthor != currentUser.id) {
                         return@withContext Result.failure(Exception("Not authorized to delete this comment"))
                    }
                } else {
                    return@withContext Result.failure(Exception("Not authorized to delete this comment"))
                }
            }


            client.from("comments")
                .update({ set("is_deleted", true) }) {
                    filter { eq("id", commentId) }
                }


            val parentId = existingComment["parent_comment_id"]?.jsonPrimitive?.contentOrNull
            if (parentId != null) {
                updateRepliesCount(parentId, -1)
            }
            val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
            if (postId != null) {
                updatePostCommentsCount(postId, -1)
            }

            storageDatabase.commentQueries.deleteById(commentId)

            Log.d(TAG, "Comment deleted successfully: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun editComment(commentId: String, content: String): Result<CommentWithUser> {
        return updateComment(commentId, content)
    }

    suspend fun updateComment(commentId: String, newContent: String): Result<CommentWithUser> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            if (newContent.isBlank()) {
                return@withContext Result.failure(Exception("Comment cannot be empty"))
            }

            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
                ?: return@withContext Result.failure(Exception("Comment not found"))

            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull
            if (commentUserId != currentUser.id) {
                return@withContext Result.failure(Exception("Not authorized to edit this comment"))
            }

            val response = client.from("comments")
                .update({
                    set("content", newContent)
                    set("is_edited", true)
                    set("edited_at", java.time.Instant.now().toString())
                }) {
                    filter { eq("id", commentId) }
                    select(Columns.raw("*, users(uid, username, display_name, avatar, verify)"))
                }
                .decodeSingle<JsonObject>()

            val updatedComment = parseCommentFromJson(response)
                ?: return@withContext Result.failure(Exception("Failed to parse updated comment"))


            val commentEntity = CommentMapper.toSharedEntity(updatedComment.toComment(), updatedComment.user?.username, updatedComment.user?.avatar)
            commentDao.insertAll(listOf(commentEntity))

            Result.success(updatedComment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pinComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
             val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
                ?: return@withContext Result.failure(Exception("Comment not found"))

            val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
                ?: return@withContext Result.failure(Exception("Post ID not found for comment"))

            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
                ?: return@withContext Result.failure(Exception("Post not found"))

            val postAuthor = post["author_uid"]?.jsonPrimitive?.contentOrNull
            if (postAuthor != currentUser.id) {
                return@withContext Result.failure(Exception("Only post author can pin comments"))
            }

            client.from("comments")
                .update({ set("is_pinned", true) }) {
                    filter { eq("id", commentId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pin comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun hideComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            val existingComment = client.from("comments")
                .select { filter { eq("id", commentId) } }
                .decodeSingleOrNull<JsonObject>()
                ?: return@withContext Result.failure(Exception("Comment not found"))

            val commentUserId = existingComment["user_id"]?.jsonPrimitive?.contentOrNull


            var isAuthorized = commentUserId == currentUser.id

            if (!isAuthorized) {
                val postId = existingComment["post_id"]?.jsonPrimitive?.contentOrNull
                if (postId != null) {

                    val post = client.from("posts")
                        .select { filter { eq("id", postId) } }
                        .decodeSingleOrNull<JsonObject>()
                    val postAuthor = post?.get("author_uid")?.jsonPrimitive?.contentOrNull
                    isAuthorized = postAuthor == currentUser.id
                }
            }

            if (!isAuthorized) {
                return@withContext Result.failure(Exception("Not authorized to hide this comment"))
            }

            client.from("comments")
                .update({
                    set("is_hidden", true)
                    set("hidden_by", currentUser.id)
                }) {
                    filter { eq("id", commentId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reportComment(commentId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            client.from("comment_reports")
                .insert(buildJsonObject {
                    put("comment_id", commentId)
                    put("reporter_id", currentUser.id)
                    put("reason", reason)
                    put("created_at", java.time.Instant.now().toString())
                })

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun parseCommentFromJson(data: JsonObject): CommentWithUser? {
        return try {
            val user = parseUserProfileFromJson(data["users"]?.jsonObject)
            val commentId = data["id"]?.jsonPrimitive?.contentOrNull ?: return null

            val reactionSummary = emptyMap<ReactionType, Int>()
            val userReaction = null

            CommentWithUser(
                id = commentId,
                postId = data["post_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                userId = data["user_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                parentCommentId = data["parent_comment_id"]?.jsonPrimitive?.contentOrNull,
                content = data["content"]?.jsonPrimitive?.contentOrNull ?: "",
                mediaUrl = data["media_url"]?.jsonPrimitive?.contentOrNull,
                createdAt = data["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                updatedAt = data["updated_at"]?.jsonPrimitive?.contentOrNull,
                likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
                repliesCount = data["replies_count"]?.jsonPrimitive?.intOrNull ?: 0,
                isDeleted = data["is_deleted"]?.jsonPrimitive?.booleanOrNull ?: false,
                isEdited = data["is_edited"]?.jsonPrimitive?.booleanOrNull ?: false,
                isPinned = data["is_pinned"]?.jsonPrimitive?.booleanOrNull ?: false,
                user = user,
                reactionSummary = reactionSummary,
                userReaction = userReaction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse comment: ${e.message}")
            null
        }
    }

    private fun parseUserProfileFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null

        return try {
            UserProfile(
                uid = userData["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                username = userData["username"]?.jsonPrimitive?.contentOrNull ?: "",
                displayName = userData["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                email = "",
                bio = userData["bio"]?.jsonPrimitive?.contentOrNull,
                avatar = userData["avatar"]?.jsonPrimitive?.contentOrNull,
                followersCount = userData["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = userData["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                postsCount = userData["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
                status = UserStatus.fromString(userData["status"]?.jsonPrimitive?.contentOrNull),
                account_type = userData["account_type"]?.jsonPrimitive?.contentOrNull ?: "user",
                verify = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false,
                banned = userData["banned"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user profile: ${e.message}")
            null
        }
    }

    private suspend fun updateRepliesCount(commentId: String, delta: Int) {
        try {
            client.postgrest.rpc(
                "increment_replies_count",
                mapOf("comment_id" to commentId, "delta" to delta)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update replies count: ${e.message}")
        }
    }

    private suspend fun updatePostCommentsCount(postId: String, delta: Int) {
        try {
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()

            val currentCount = post?.get("comments_count")?.jsonPrimitive?.intOrNull ?: 0
            val newCount = maxOf(0, currentCount + delta)

            client.from("posts")
                .update({ set("comments_count", newCount) }) {
                    filter { eq("id", postId) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update post comments count: ${e.message}")
        }
    }

    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"

        Log.e(TAG, "Supabase error: $message", exception)

        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Comment not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) ->
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) ->
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            message.contains("54001") -> "Server Configuration Error: Stack depth limit exceeded. Please contact support."
            else -> "Failed to process comment: $message"
        }
    }

    private suspend fun processMentions(
        postId: String,
        commentId: String,
        content: String,
        senderId: String,
        parentCommentId: String?
    ) {
        try {
            val mentionedUsers = com.synapse.social.studioasinc.core.domain.parser.MentionParser.extractMentions(content)

            if (mentionedUsers.isNotEmpty()) {
                Log.d(TAG, "Processing mentions: $mentionedUsers")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process mentions: ${e.message}", e)
        }
    }
}

private fun CommentWithUser.toComment(): Comment {
    return Comment(
        key = this.id,
        postKey = this.postId,
        uid = this.userId,
        comment = this.content,
        push_time = this.createdAt.toString(),
        replyCommentKey = this.parentCommentId
    )
}
