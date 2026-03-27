package com.synapse.social.studioasinc.domain.repository

import androidx.paging.PagingData
import com.synapse.social.studioasinc.domain.model.FeedItem
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun getPost(postId: String): Result<Post?>
    suspend fun createPost(post: Post): Result<Post>
    suspend fun createPosts(posts: List<Post>): Result<List<Post>>
    suspend fun updatePost(post: Post): Result<Post>
    suspend fun resharePost(postId: String): Result<Unit>
    suspend fun unresharePost(postId: String): Result<Unit>
    suspend fun quotePost(postId: String, text: String): Result<Post>
    fun getPostsPaged(): Flow<PagingData<Post>>
    fun getFeedPaged(): Flow<PagingData<FeedItem>>
}
