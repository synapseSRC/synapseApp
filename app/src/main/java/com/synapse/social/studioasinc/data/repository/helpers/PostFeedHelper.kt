package com.synapse.social.studioasinc.data.repository.helpers

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.synapse.social.studioasinc.data.paging.FeedPagingSource
import com.synapse.social.studioasinc.data.paging.PostPagingSource
import com.synapse.social.studioasinc.domain.model.FeedItem
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.data.repository.PostMapper
import com.synapse.social.studioasinc.data.repository.PostSelectDto
import com.synapse.social.studioasinc.data.repository.toDomain
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PostFeedHelper(
    private val postDao: PostDao,
    private val client: JanSupabaseClient,
    private val offlineActionRepository: OfflineActionRepository,
    private val utils: PostRepositoryUtils
) {

    private val reactionRepository = com.synapse.social.studioasinc.data.repository.ReactionRepository(client)
    private val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository(client)

    fun getPostsPaged(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PostPagingSource(client.from("posts")) }
        ).flow
    }

    fun getFeedPaged(): Flow<PagingData<FeedItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { FeedPagingSource(client, postDao) }
        ).flow
    }

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
                    android.util.Log.e(PostRepositoryUtils.TAG, "Background refresh failed", result.exceptionOrNull())
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
                postDto.toDomain(PostRepositoryUtils.Companion::constructMediaUrl, PostRepositoryUtils.Companion::constructAvatarUrl).also { post ->
                    postDto.user?.let { user ->
                        if (user.uid.isNotEmpty()) {
                            utils.profileCache[user.uid] = CacheEntry(
                                ProfileData(
                                    username = if (!user.displayName.isNullOrBlank()) user.displayName else user.username,
                                    avatarUrl = user.avatarUrl?.let { PostRepositoryUtils.constructAvatarUrl(it) },
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to fetch user posts: ${e.message}", e)
            Result.failure(e)
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
                postDto.toDomain(PostRepositoryUtils.Companion::constructMediaUrl, PostRepositoryUtils.Companion::constructAvatarUrl).also { post ->
                    // Cache user profile
                     postDto.user?.let { user ->
                        if (user.uid.isNotEmpty()) {
                            utils.profileCache[user.uid] = CacheEntry(
                                ProfileData(
                                    username = if (!user.displayName.isNullOrBlank()) user.displayName else user.username,
                                    avatarUrl = user.avatarUrl?.let { PostRepositoryUtils.constructAvatarUrl(it) },
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to fetch user posts: ${e.message}", e)
            Result.failure(e)
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
}
