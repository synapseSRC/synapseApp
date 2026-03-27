package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.repository.PostMapper
import com.synapse.social.studioasinc.data.repository.toInsertDto
import com.synapse.social.studioasinc.data.repository.toUpdateDto
import com.synapse.social.studioasinc.data.repository.PostInsertDto
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive

internal class PostCrudHelper(
    private val postDao: PostDao,
    private val client: JanSupabaseClient,
    private val offlineActionRepository: OfflineActionRepository,
    private val utils: PostRepositoryUtils
) {

    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured."))
            }

            if (post.username == null) {
                val profile = utils.fetchUserProfile(post.authorUid)
                if (profile != null) {
                    post.username = profile.username
                    post.avatarUrl = profile.avatarUrl
                    post.isVerified = profile.isVerified
                }
            }

            val postDto = post.toInsertDto()

            android.util.Log.d(PostRepositoryUtils.TAG, "Creating post with DTO fields: ${getFieldNames(postDto)}")
            android.util.Log.d(PostRepositoryUtils.TAG, "Post author_uid: ${postDto.authorUid}")
            android.util.Log.d(PostRepositoryUtils.TAG, "Current auth user: ${client.auth.currentUserOrNull()?.id}")

            client.from("posts").insert(postDto)
            postDao.insert(PostMapper.toEntity(post))
            processMentions(post.id, post.postText ?: "", post.authorUid)

            android.util.Log.d(PostRepositoryUtils.TAG, "Post created successfully: ${post.id}")
            Result.success(post)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to create post", e)
            Result.failure(Exception(PostRepositoryUtils.mapSupabaseError(e)))
        }
    }

    suspend fun createPosts(posts: List<Post>): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured."))
            }

            if (posts.isEmpty()) return@withContext Result.success(emptyList())

            val enrichedPosts = posts.map { post ->
                if (post.username == null) {
                    val profile = utils.fetchUserProfile(post.authorUid)
                    if (profile != null) {
                        post.username = profile.username
                        post.avatarUrl = profile.avatarUrl
                        post.isVerified = profile.isVerified
                    }
                }
                post
            }

            val postDtos = enrichedPosts.map { it.toInsertDto() }

            android.util.Log.d(PostRepositoryUtils.TAG, "Creating ${postDtos.size} posts in batch")
            client.from("posts").insert(postDtos)
            postDao.insertAll(enrichedPosts.map { PostMapper.toEntity(it) })

            enrichedPosts.forEach { post ->
                processMentions(post.id, post.postText ?: "", post.authorUid)
            }

            android.util.Log.d(PostRepositoryUtils.TAG, "Batch posts created successfully")
            Result.success(enrichedPosts)
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to create posts in batch", e)
            Result.failure(Exception(PostRepositoryUtils.mapSupabaseError(e)))
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
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocalPost(post: Any): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (post is Post) {
                postDao.insert(PostMapper.toEntity(post))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to update local post", e)
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
                    utils.fetchUserProfile(model.authorUid)?.let { profile ->
                        model.username = profile.username
                        model.avatarUrl = profile.avatarUrl
                        model.isVerified = profile.isVerified
                    }
                }
                model
            }
            Result.success(post)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Error getting post from database: ${e.message}"))
        }
    }

    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").update(updates) {
                filter { eq("id", postId) }
            }
            Result.success(Post(id = postId, authorUid = ""))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to update post", e)
            Result.failure(Exception(PostRepositoryUtils.mapSupabaseError(e)))
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to update full post", e)
            Result.failure(Exception(PostRepositoryUtils.mapSupabaseError(e)))
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("posts").delete {
                filter { eq("id", postId) }
            }
            postDao.deleteById(postId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to delete post", e)
            Result.failure(Exception(PostRepositoryUtils.mapSupabaseError(e)))
        }
    }

    suspend fun toggleComments(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val post = client.from("posts").select(Columns.list("post_disable_comments")) {
                filter { eq("id", postId) }
            }.decodeSingleOrNull<JsonObject>()

            val currentStr = post?.get("post_disable_comments")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
            val currentBool = currentStr == "true"
            val newStr = if (currentBool) "false" else "true"

            client.from("posts").update(mapOf("post_disable_comments" to newStr)) {
                filter { eq("id", postId) }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
             android.util.Log.e(PostRepositoryUtils.TAG, "Failed to toggle comments", e)
             Result.failure(e)
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
                android.util.Log.d(PostRepositoryUtils.TAG, "Processing mentions: $mentionedUsers")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to process mentions: ${e.message}", e)
        }
    }
}
