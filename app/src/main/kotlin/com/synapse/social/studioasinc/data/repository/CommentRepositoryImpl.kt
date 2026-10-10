package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.data.source.CommentLocalDataSource
import com.synapse.social.studioasinc.data.source.CommentRemoteDataSource
import com.synapse.social.studioasinc.domain.model.*
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton

class CommentRepositoryImpl constructor(
    private val localDataSource: CommentLocalDataSource,
    private val remoteDataSource: CommentRemoteDataSource,
    private val userRepository: UserRepositoryImpl,
    @Named("ApplicationScope") private val externalScope: CoroutineScope,
    private val reactionRepository: ReactionRepositoryImpl
) {

    companion object {
        private const val TAG = "CommentRepositoryImpl"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }

    fun getComments(postId: String): Flow<Result<List<Comment>>> {
        return flow {
            try {
                val comments = localDataSource.getCommentsByPostIdFromSqlDelight(postId)
                emit(Result.success(comments.map { CommentMapper.toModel(it) }))
            } catch (e: Exception) {
                emit(Result.failure(Exception("Error getting comments from database: ${e.message}")))
            }
        }
    }

    suspend fun fetchComments(postId: String, limit: Int = 50, offset: Int = 0): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching comments for post: $postId")

            val comments = remoteDataSource.fetchComments(postId, limit, offset)
            val populatedComments = reactionRepository.populateCommentReactions(comments)

            // Cache in DB
            val commentsToCache = populatedComments.map {
                CommentMapper.toSharedEntity(it.toComment(), it.user?.username, it.user?.avatar)
            }
            localDataSource.insertAll(commentsToCache)

            Result.success(populatedComments)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comments: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun refreshComments(postId: String, limit: Int = 50, offset: Int = 0): Result<Unit> {
        return fetchComments(postId, limit, offset).map { Unit }
    }

    suspend fun getComment(commentId: String): Result<CommentWithUser> = withContext(Dispatchers.IO) {
        try {
            val comment = remoteDataSource.getComment(commentId)
                ?: return@withContext Result.failure(Exception("Comment not found or failed to parse"))

            val populatedComments = reactionRepository.populateCommentReactions(listOf(comment))

            Result.success(populatedComments.first())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comment: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getCommentAncestors(commentId: String): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            val ancestors = mutableListOf<CommentWithUser>()
            var currentComment = getComment(commentId).getOrNull()
            var currentParentId = currentComment?.parentCommentId
            
            var depth = 0
            while (currentParentId != null && depth < 10) {
                val parentComment = getComment(currentParentId).getOrNull()
                if (parentComment != null) {
                    ancestors.add(parentComment)
                    currentParentId = parentComment.parentCommentId
                    depth++
                } else {
                    break
                }
            }
            
            Result.success(ancestors.reversed())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comment ancestors: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun fetchPagedReplies(parentId: String, limit: Int = 50, offset: Int = 0): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching paged replies for comment: $parentId")

            val replies = remoteDataSource.fetchReplies(parentId, limit, offset)
            val populatedReplies = reactionRepository.populateCommentReactions(replies)

            val commentsToCache = populatedReplies.map {
                CommentMapper.toSharedEntity(it.toComment(), it.user?.username, it.user?.avatar)
            }
            localDataSource.insertAll(commentsToCache)

            Result.success(populatedReplies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch paged replies: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun fetchReplies(parentCommentId: String, limit: Int = 20, offset: Int = 0): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching paginated replies for comment: $parentCommentId (limit: $limit, offset: $offset)")

            val replies = remoteDataSource.fetchReplies(parentCommentId, limit, offset)

            Log.d(TAG, "Raw response size: ${replies.size}")

            // Optimized: Bulk fetch reactions
            val populatedReplies = reactionRepository.populateCommentReactions(replies)

            Log.d(TAG, "Successfully parsed ${populatedReplies.size} paginated replies")

            val commentsToCache = populatedReplies.map {
                CommentMapper.toSharedEntity(it.toComment(), it.user?.username, it.user?.avatar)
            }
            localDataSource.insertAll(commentsToCache)

            Result.success(populatedReplies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch paginated replies: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getReplies(commentId: String): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching replies for comment: $commentId")

            val replies = remoteDataSource.fetchAllReplies(commentId)

            Log.d(TAG, "Raw response size: ${replies.size}")

            // Optimized: Bulk fetch reactions
            val populatedReplies = reactionRepository.populateCommentReactions(replies)

            Log.d(TAG, "Successfully parsed ${populatedReplies.size} replies")

            val commentsToCache = populatedReplies.map {
                CommentMapper.toSharedEntity(it.toComment(), it.user?.username, it.user?.avatar)
            }
            localDataSource.insertAll(commentsToCache)

            Result.success(populatedReplies)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch paged comments: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun fetchUserComments(userId: String, limit: Int = 20, offset: Int = 0): Result<List<CommentWithUser>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching comments for user: $userId")

            val comments = remoteDataSource.fetchUserComments(userId, limit, offset)
            val populatedComments = reactionRepository.populateCommentReactions(comments)

            Result.success(populatedComments)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user comments: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    fun getCommentsForPost(postId: String): Flow<List<CommentWithUser>> {
        return localDataSource.getCommentsForPostFlow(postId).map { entities ->
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
                    createdAt = comment.pushTime,
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
        return addComment(postId, content, mediaUrl, parentId)
    }

    suspend fun addComment(postId: String, content: String, mediaUrl: String? = null, parentCommentId: String? = null): Result<CommentWithUser> = withContext(Dispatchers.IO) {
        try {
            val currentUser = remoteDataSource.getCurrentUser()
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
                    val comment = try {
                        remoteDataSource.addComment(
                            id = clientGeneratedId,
                            postId = postId,
                            userId = userId,
                            content = content,
                            mediaUrl = mediaUrl,
                            parentCommentId = parentCommentId
                        )
                    } catch (e: RestException) {
                         throw e
                    }

                    if (comment == null) {
                        return@withContext Result.failure(Exception("Failed to create or parse comment"))
                    }

                    resultComment = comment

                    // Cache in DB
                    val commentEntity = CommentMapper.toSharedEntity(comment.toComment(), comment.user?.username, comment.user?.avatar)
                    localDataSource.insertAll(listOf(commentEntity))

                    externalScope.launch {
                        if (parentCommentId != null) {
                            remoteDataSource.updateRepliesCount(parentCommentId, 1)
                        }
                    }
                    externalScope.launch {
                        processMentions(postId, comment.id, content, userId, parentCommentId)
                    }
                    externalScope.launch {
                         remoteDataSource.updatePostCommentsCount(postId, 1)
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
                resultComment?.let { Result.success(it) } ?: Result.failure(Exception("Comment null"))
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
            val currentUser = remoteDataSource.getCurrentUser()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            val comment = remoteDataSource.getComment(commentId)
                ?: return@withContext Result.failure(Exception("Comment not found"))

            val commentUserId = comment.userId
            val postId = comment.postId

            if (commentUserId != currentUser.id) {
                val postAuthor = remoteDataSource.getPostAuthorId(postId)

                if (postAuthor != currentUser.id) {
                     return@withContext Result.failure(Exception("Not authorized to delete this comment"))
                }
            }

            remoteDataSource.markCommentDeleted(commentId)

            val parentId = comment.parentCommentId
            if (parentId != null) {
                remoteDataSource.updateRepliesCount(parentId, -1)
            }
            remoteDataSource.updatePostCommentsCount(postId, -1)

            localDataSource.deleteByIdFromSqlDelight(commentId)

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
            val currentUser = remoteDataSource.getCurrentUser()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            if (newContent.isBlank()) {
                return@withContext Result.failure(Exception("Comment cannot be empty"))
            }

            val comment = remoteDataSource.getComment(commentId)
                ?: return@withContext Result.failure(Exception("Comment not found"))

            if (comment.userId != currentUser.id) {
                return@withContext Result.failure(Exception("Not authorized to edit this comment"))
            }

            val updatedComment = remoteDataSource.updateComment(commentId, newContent)
                ?: return@withContext Result.failure(Exception("Failed to update or parse comment"))

            val commentEntity = CommentMapper.toSharedEntity(updatedComment.toComment(), updatedComment.user?.username, updatedComment.user?.avatar)
            localDataSource.insertAll(listOf(commentEntity))

            Result.success(updatedComment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun pinComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
             val currentUser = remoteDataSource.getCurrentUser()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            val comment = remoteDataSource.getComment(commentId)
                ?: return@withContext Result.failure(Exception("Comment not found"))

            val postId = comment.postId

            val postAuthor = remoteDataSource.getPostAuthorId(postId)
            if (postAuthor != currentUser.id) {
                return@withContext Result.failure(Exception("Only post author can pin comments"))
            }

            remoteDataSource.pinComment(commentId)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pin comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun hideComment(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = remoteDataSource.getCurrentUser()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            val comment = remoteDataSource.getComment(commentId)
                ?: return@withContext Result.failure(Exception("Comment not found"))

            var isAuthorized = comment.userId == currentUser.id

            if (!isAuthorized) {
                val postId = comment.postId
                val postAuthor = remoteDataSource.getPostAuthorId(postId)
                isAuthorized = postAuthor == currentUser.id
            }

            if (!isAuthorized) {
                return@withContext Result.failure(Exception("Not authorized to hide this comment"))
            }

            remoteDataSource.hideComment(commentId, currentUser.id)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun reportComment(commentId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = remoteDataSource.getCurrentUser()
                ?: return@withContext Result.failure(Exception("User must be authenticated"))

            remoteDataSource.reportComment(commentId, currentUser.id, reason)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report comment: ${e.message}", e)
            Result.failure(e)
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
        pushTime = this.createdAt.toString(),
        replyCommentKey = this.parentCommentId
    )
}
