package com.synapse.social.studioasinc.data.repository

import android.content.SharedPreferences
import kotlinx.datetime.Instant


import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.UserReaction
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import java.util.concurrent.ConcurrentHashMap
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.synapse.social.studioasinc.data.paging.PostPagingSource
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

import io.github.jan.supabase.SupabaseClient as JanSupabaseClient

@Singleton
class PostRepository constructor(
    private val postDao: PostDao,
    private val client: JanSupabaseClient
) : PostActionsRepository {

    fun getPostsPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PostPagingSource(client.from("posts")) }
        ).flow
    }

    fun getFeedPaged(): Flow<PagingData<com.synapse.social.studioasinc.domain.model.FeedItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { com.synapse.social.studioasinc.data.paging.FeedPagingSource(client) }
        ).flow
    }

    // Cache for user profiles to avoid repeated network calls
    private val profileCache = ConcurrentHashMap<String, CacheEntry<ProfileData>>()

    private data class CacheEntry<T>(val data: T, val timestamp: Long = System.currentTimeMillis()) {
        fun isExpired() = System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_MS
    }

    // Using a simple cache for user profiles to reduce N+1 queries
    private val userProfileCache = ConcurrentHashMap<String, Result<ProfileData>>()

    companion object {
        private const val PREF_LAST_SYNC_TIME = "last_post_sync_time"
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
        private const val TAG = "PostRepository"
        private val PGRST_REGEX = Regex("PGRST\\d+")
        private val COLUMN_REGEX = Regex("column \"([^\"]+)\"")

        internal fun findDeletedIds(localChunk: List<String>, serverResponse: List<JsonObject>): List<String> {
            val serverIds = serverResponse.mapNotNull { it["id"]?.jsonPrimitive?.contentOrNull }.toSet()

            // Hard deleted: in localChunk but not in serverIds
            val missingIds = localChunk.filter { !serverIds.contains(it) }

            // Soft deleted: in serverResponse with is_deleted=true
            val softDeletedIds = serverResponse.filter {
                it["is_deleted"]?.jsonPrimitive?.booleanOrNull == true
            }.mapNotNull { it["id"]?.jsonPrimitive?.contentOrNull }

            return (missingIds + softDeletedIds).distinct()
        }
    }

    private data class ProfileData(
        val username: String?,
        val avatarUrl: String?,
        val isVerified: Boolean
    )

    fun constructMediaUrl(storagePath: String): String {
        return SupabaseClient.constructMediaUrl(storagePath)
    }

    private fun constructAvatarUrl(storagePath: String): String {
        return SupabaseClient.constructAvatarUrl(storagePath)
    }

    private fun mapSupabaseError(exception: Exception): String {
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

    private fun extractColumnInfo(message: String): String {
        val columnMatch = COLUMN_REGEX.find(message)
        return columnMatch?.groupValues?.get(1) ?: "unknown column"
    }



    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured."))
            }


            if (post.username == null) {
                val profile = fetchUserProfile(post.authorUid)
                if (profile != null) {
                    post.username = profile.username
                    post.avatarUrl = profile.avatarUrl
                    post.isVerified = profile.isVerified
                }
            }

            val postDto = post.toInsertDto()

            android.util.Log.d(TAG, "Creating post with DTO fields: ${getFieldNames(postDto)}")
            android.util.Log.d(TAG, "Post author_uid: ${postDto.authorUid}")
            android.util.Log.d(TAG, "Current auth user: ${client.auth.currentUserOrNull()?.id}")

            client.from("posts").insert(postDto)
            postDao.insert(PostMapper.toEntity(post))
            processMentions(post.id, post.postText ?: "", post.authorUid)

            android.util.Log.d(TAG, "Post created successfully: ${post.id}")
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun resharePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            
            val newPostId = java.util.UUID.randomUUID().toString()
            client.from("posts").insert(mapOf(
                "id" to newPostId,
                "author_uid" to userId,
                "quoted_post_id" to postId,
                "is_quote" to false,
                "timestamp" to System.currentTimeMillis()
            ))
            
            client.postgrest.rpc("increment_post_reshares", mapOf("post_id" to postId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun quotePost(postId: String, text: String): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val post = Post(
                id = java.util.UUID.randomUUID().toString(),
                authorUid = userId,
                postText = text,
                quotedPostId = postId,
                isQuote = true
            )
            createPost(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocalPost(post: Any): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (post is Post) {
                postDao.insert(PostMapper.toEntity(post))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update local post", e)
            Result.failure(Exception("Failed to update local post"))
        }
    }

    private fun getFieldNames(dto: PostInsertDto): String {
        return "id, key, author_uid, post_text, post_image, post_type, post_visibility, " +
               "post_hide_views_count, post_hide_like_count, post_hide_comments_count, " +
               "post_disable_comments, publish_date, timestamp, likes_count, comments_count, " +
               "views_count, reshares_count, media_items, has_poll, poll_question, poll_options, " +
               "poll_end_time, poll_allow_multiple, has_location, location_name, location_address, " +
               "location_latitude, location_longitude, location_place_id, youtube_url"
    }

    suspend fun getPost(postId: String): Result<Post?> = withContext(Dispatchers.IO) {
        try {
            val post = postDao.getPostById(postId)?.let { entity ->
                val model = PostMapper.toModel(entity)

                if (model.username == null) {
                    fetchUserProfile(model.authorUid)?.let { profile ->
                        model.username = profile.username
                        model.avatarUrl = profile.avatarUrl
                        model.isVerified = profile.isVerified
                    }
                }
                model
            }
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(Exception("Error getting post from database: ${e.message}"))
        }
    }

    // Renamed to avoid confusion with paged version, but original was just getPosts() returning Flow<Result<List<Post>>>
    fun getPosts(): Flow<Result<List<Post>>> = flow {
        // Observe local DB
        val dbFlow = postDao.getAllPostsAsFlow().map { entities ->
            val posts = entities.map { PostMapper.toModel(it) }
            Result.success(posts)
        }

        // Trigger refresh and emit DB flow
        coroutineScope {
            launch {
                val result = refreshPosts(0, 20)
                if (result.isFailure) {
                    android.util.Log.e(TAG, "Background refresh failed", result.exceptionOrNull())
                }
            }
            emitAll(dbFlow)
        }
    }

    fun getReelsPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PostPagingSource(client.from("posts")) }
        ).flow
    }

    suspend fun refreshPosts(page: Int, pageSize: Int): Result<Unit> {
        return try {
            val offset = page * pageSize

            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!author_uid(uid, username, display_name, avatar, verify),
                        latest_comments:comments(id, content, user_id, created_at, users(username)),
                        quoted_post:posts!quoted_post_id(*, users!author_uid(uid, username, display_name, avatar, verify))
                    """.trimIndent())
                ) {
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<PostSelectDto>()

            val posts = response.map { postDto ->
                postDto.toDomain(::constructMediaUrl, ::constructAvatarUrl).also { post ->

                    postDto.user?.let { user ->
                        if (user.uid.isNotEmpty()) {
                            profileCache[user.uid] = CacheEntry(
                                ProfileData(
                                    username = if (!user.displayName.isNullOrBlank()) user.displayName else user.username,
                                    avatarUrl = user.avatarUrl?.let { constructAvatarUrl(it) },
                                    isVerified = user.isVerified ?: false
                                )
                            )
                        }
                    }
                }
            }

            val postsWithReactions = populatePostReactions(posts)
            val postsWithPolls = populatePostPolls(postsWithReactions)

            // Insert into DB
            // Batch insert using Dao
            val entities = postsWithPolls.map { PostMapper.toEntity(it) }
            postDao.insertAll(entities)

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user posts: ${e.message}", e)
            Result.failure(e)
        }
    }
    private suspend fun syncDeletedPosts() = coroutineScope {
        try {
            val localIds = postDao.getAllPostIds()
            if (localIds.isEmpty()) return@coroutineScope

            val semaphore = Semaphore(5)

            // Process in chunks of 50 to respect URL length limits (approx 2KB max for safe GET)
            // 50 UUIDs * 36 chars = 1800 chars + overhead
            val idsToDelete = localIds.chunked(50).map { chunk ->
                async {
                    semaphore.withPermit {
                        try {
                            val response = client.from("posts")
                                .select(columns = Columns.raw("id, is_deleted")) {
                                    filter { isIn("id", chunk) }
                                }
                                .decodeList<JsonObject>()
                            findDeletedIds(chunk, response)
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Failed to check chunk existence", e)
                            emptyList<String>()
                        }
                    }
                }
            }.awaitAll().flatten()

            if (idsToDelete.isNotEmpty()) {
                val uniqueIdsToDelete = idsToDelete.distinct()
                android.util.Log.d(TAG, "Syncing deletions: removing ${uniqueIdsToDelete.size} posts")
                postDao.deleteByIds(uniqueIdsToDelete)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync deleted posts", e)
        }
    }
    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!author_uid(uid, username, display_name, avatar, verify),
                        latest_comments:comments(id, content, user_id, created_at, users(username)),
                        quoted_post:posts!quoted_post_id(*, users!author_uid(uid, username, display_name, avatar, verify))
                    """.trimIndent())
                ) {
                    filter { eq("author_uid", userId) }
                    order("timestamp", order = Order.DESCENDING)
                }
                .decodeList<PostSelectDto>()
            val posts = response.map { postDto ->
                postDto.toDomain(::constructMediaUrl, ::constructAvatarUrl).also { post ->
                    // Cache user profile
                     postDto.user?.let { user ->
                        if (user.uid.isNotEmpty()) {
                            profileCache[user.uid] = CacheEntry(
                                ProfileData(
                                    username = if (!user.displayName.isNullOrBlank()) user.displayName else user.username,
                                    avatarUrl = user.avatarUrl?.let { constructAvatarUrl(it) },
                                    isVerified = user.isVerified ?: false
                                )
                            )
                        }
                    }
                }
            }
            val postsWithReactions = populatePostReactions(posts)
            val postsWithPolls = populatePostPolls(postsWithReactions)
            Result.success(postsWithPolls)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user posts: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").update(updates) {
                filter { eq("id", postId) }
            }
            Result.success(Post(id = postId, authorUid = ""))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    suspend fun updatePost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val updateDto = post.toUpdateDto()
            client.from("posts").update(updateDto) {
                filter { eq("id", post.id) }
            }
            postDao.insert(PostMapper.toEntity(post))
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to update full post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    override suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").delete {
                filter { eq("id", postId) }
            }
            postDao.deleteById(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to delete post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    private val reactionRepository = ReactionRepository()
    private val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()

    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType,
        oldReaction: ReactionType? = null,
        skipCheck: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {

        reactionRepository.toggleReaction(postId, "post", reactionType, oldReaction, skipCheck)
            .map { Unit }
    }

    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> =
        reactionRepository.getReactionSummary(postId, "post")

    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> =
        if (userId == client.auth.currentUserOrNull()?.id) {
             reactionRepository.getUserReaction(postId, "post")
        } else {
             withContext(Dispatchers.IO) {
                 try {
                     val reaction = client.from("reactions")
                         .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                         .decodeSingleOrNull<JsonObject>()
                     val typeStr = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull
                     Result.success(typeStr?.let { ReactionType.fromString(it) })
                 } catch (e: Exception) {
                     Result.failure(Exception("Error fetching user reaction"))
                 }
             }
        }

    suspend fun getUsersWhoReacted(
        postId: String,
        reactionType: ReactionType? = null
    ): Result<List<UserReaction>> = withContext(Dispatchers.IO) {
        try {

            val reactions = client.from("reactions")
                .select(Columns.raw("*, users!inner(uid, username, display_name, avatar, verify)")) {
                    filter {
                        eq("post_id", postId)
                        if (reactionType != null) eq("reaction_type", reactionType.name)
                    }
                }
                .decodeList<JsonObject>()

            val userReactions = reactions.mapNotNull { reaction ->
                val userId = reaction["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val user = reaction["users"]?.jsonObject
                val dName = user?.get("display_name")?.jsonPrimitive?.contentOrNull
                UserReaction(
                    userId = userId,
                    username = if (!dName.isNullOrBlank()) dName else (user?.get("username")?.jsonPrimitive?.contentOrNull ?: "Unknown"),
                    profileImage = user?.get("avatar")?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user?.get("verify")?.jsonPrimitive?.booleanOrNull ?: false,
                    reactionType = reaction["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE",
                    reactedAt = reaction["created_at"]?.jsonPrimitive?.contentOrNull
                )
            }
            Result.success(userReactions)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }


    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        return reactionRepository.populatePostReactions(posts)
    }

    private suspend fun populatePostPolls(posts: List<Post>): List<Post> {
        val pollPosts = posts.filter { it.hasPoll == true }
        if (pollPosts.isEmpty()) return posts

        val postIds = pollPosts.map { it.id }


        val userVotesResult = pollRepository.getBatchUserVotes(postIds)
        val userVotes = userVotesResult.getOrNull() ?: emptyMap()


        val pollCountsResult = pollRepository.getBatchPollVotes(postIds)
        val pollCounts = pollCountsResult.getOrNull() ?: emptyMap()

        return posts.map { post ->
            if (post.hasPoll == true) {
                val userVote = userVotes[post.id]
                val counts = pollCounts[post.id] ?: emptyMap()


                val updatedOptions = post.pollOptions?.mapIndexed { index, option ->
                    option.copy(votes = counts[index] ?: 0)
                }

                val updatedPost = post.copy(
                    pollOptions = updatedOptions
                )
                updatedPost.userPollVote = userVote
                updatedPost
            } else {
                post
            }
        }
    }

    private suspend fun fetchUserProfilesBatch(userIds: List<String>) {
        try {
            val users = client.from("users").select {
                filter { isIn("uid", userIds) }
            }.decodeList<JsonObject>()

            users.forEach { user ->
                val uid = user["uid"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val dName = user["display_name"]?.jsonPrimitive?.contentOrNull
                val profile = ProfileData(
                    username = if (!dName.isNullOrBlank()) dName else user["username"]?.jsonPrimitive?.contentOrNull,
                    avatarUrl = user["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user["verify"]?.jsonPrimitive?.booleanOrNull ?: false
                )
                profileCache[uid] = CacheEntry(profile)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to batch fetch user profiles", e)
        }
    }

    private suspend fun fetchUserProfile(userId: String): ProfileData? {

        profileCache[userId]?.let { entry ->
            if (!entry.isExpired()) return entry.data
        }

        return try {
            val user = client.from("users").select {
                filter { eq("uid", userId) }
            }.decodeSingleOrNull<JsonObject>()

            if (user != null) {
                val dName = user["display_name"]?.jsonPrimitive?.contentOrNull
                val profile = ProfileData(
                    username = if (!dName.isNullOrBlank()) dName else user["username"]?.jsonPrimitive?.contentOrNull,
                    avatarUrl = user["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user["verify"]?.jsonPrimitive?.booleanOrNull ?: false
                )
                profileCache[userId] = CacheEntry(profile)
                profile
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user profile for $userId", e)
            null
        }
    }

    private suspend fun processMentions(
        postId: String,
        content: String,
        senderId: String
    ) {
        try {

            val mentionedUsers = com.synapse.social.studioasinc.core.domain.parser.MentionParser.extractMentions(content)


            if (mentionedUsers.isNotEmpty()) {
                android.util.Log.d(TAG, "Processing mentions: $mentionedUsers")

            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to process mentions: ${e.message}", e)
        }
    }

    override suspend fun toggleComments(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val post = client.from("posts").select(Columns.list("post_disable_comments")) {
                filter { eq("id", postId) }
            }.decodeSingleOrNull<JsonObject>()

            val currentStr = post?.get("post_disable_comments")?.jsonPrimitive?.contentOrNull
            val currentBool = currentStr == "true"
            val newStr = if (currentBool) "false" else "true"

            client.from("posts").update(mapOf("post_disable_comments" to newStr)) {
                filter { eq("id", postId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
             android.util.Log.e(TAG, "Failed to toggle comments", e)
             Result.failure(e)
        }
    }
}
