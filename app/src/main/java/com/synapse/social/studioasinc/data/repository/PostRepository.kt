package com.synapse.social.studioasinc.data.repository

import androidx.paging.PagingData
import com.synapse.social.studioasinc.data.repository.helpers.PostCrudHelper
import com.synapse.social.studioasinc.data.repository.helpers.PostFeedHelper
import com.synapse.social.studioasinc.data.repository.helpers.PostReactionHelper
import com.synapse.social.studioasinc.data.repository.helpers.PostRepositoryUtils
import com.synapse.social.studioasinc.data.repository.helpers.PostSyncHelper
import com.synapse.social.studioasinc.domain.model.FeedItem
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.UserReaction
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val postDao: PostDao,
    private val client: JanSupabaseClient,
    private val offlineActionRepository: OfflineActionRepository
) : PostActionsRepository, com.synapse.social.studioasinc.domain.repository.PostRepository {

    private val utils = PostRepositoryUtils(client)
    private val crudHelper = PostCrudHelper(postDao, client, offlineActionRepository, utils)
    private val feedHelper = PostFeedHelper(postDao, client, offlineActionRepository, utils)
    private val reactionHelper = PostReactionHelper(postDao, client, offlineActionRepository)
    private val syncHelper = PostSyncHelper(postDao, client, offlineActionRepository)

    override fun getPostsPaged(): Flow<PagingData<Post>> = feedHelper.getPostsPaged()
    override fun getFeedPaged(): Flow<PagingData<FeedItem>> = feedHelper.getFeedPaged()
    fun getReelsPaged(): Flow<PagingData<Post>> = feedHelper.getReelsPaged()
    fun getPosts(): Flow<Result<List<Post>>> = feedHelper.getPosts()
    suspend fun refreshPosts(page: Int, pageSize: Int): Result<Unit> = feedHelper.refreshPosts(page, pageSize)
    suspend fun getUserPosts(userId: String): Result<List<Post>> = feedHelper.getUserPosts(userId)

    override suspend fun createPost(post: Post): Result<Post> = crudHelper.createPost(post)
    override suspend fun createPosts(posts: List<Post>): Result<List<Post>> = crudHelper.createPosts(posts)
    override suspend fun updateLocalPost(post: Any): Result<Unit> = crudHelper.updateLocalPost(post)
    override suspend fun getPost(postId: String): Result<Post?> = crudHelper.getPost(postId)
    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> = crudHelper.updatePost(postId, updates)
    override suspend fun updatePost(post: Post): Result<Post> = crudHelper.updatePost(post)
    override suspend fun deletePost(postId: String): Result<Unit> = crudHelper.deletePost(postId)
    override suspend fun toggleComments(postId: String): Result<Unit> = crudHelper.toggleComments(postId)
    override suspend fun resharePost(postId: String): Result<Unit> = crudHelper.resharePost(postId)
    override suspend fun quotePost(postId: String, text: String): Result<Post> = crudHelper.quotePost(postId, text)

    suspend fun toggleReaction(postId: String, userId: String, reactionType: ReactionType, oldReaction: ReactionType? = null, skipCheck: Boolean = false): Result<Unit> =
        reactionHelper.toggleReaction(postId, userId, reactionType, oldReaction, skipCheck)
    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = reactionHelper.getReactionSummary(postId)
    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = reactionHelper.getUserReaction(postId, userId)
    suspend fun getUsersWhoReacted(postId: String, reactionType: ReactionType? = null): Result<List<UserReaction>> = reactionHelper.getUsersWhoReacted(postId, reactionType)

    private suspend fun syncDeletedPosts() = syncHelper.syncDeletedPosts()

    fun constructMediaUrl(storagePath: String): String = SupabaseClient.constructMediaUrl(storagePath)

    companion object {
        internal fun findDeletedIds(localChunk: List<String>, serverResponse: List<JsonObject>): List<String> {
            val serverIds = serverResponse.mapNotNull { it["id"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull }.toSet()
            val missingIds = localChunk.filter { !serverIds.contains(it) }
            val softDeletedIds = serverResponse.filter {
                it["is_deleted"]?.let { if (it is JsonPrimitive) it else null }?.booleanOrNull == true
            }.mapNotNull { it["id"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull }
            return (missingIds + softDeletedIds).distinct()
        }
    }
}
