package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import com.synapse.social.studioasinc.shared.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

interface PostDao {
    suspend fun insert(post: PostEntity)
    suspend fun insertAll(posts: List<PostEntity>)
    suspend fun getPostById(id: String): PostEntity?
    suspend fun getAllPosts(): List<PostEntity>
    suspend fun getPostsPaged(limit: Long, offset: Long): List<PostEntity>
    suspend fun getPostsCount(): Long
    fun getAllPostsAsFlow(): Flow<List<PostEntity>>
    suspend fun getAllPostIds(): List<String>
    suspend fun deleteById(id: String)
    suspend fun deleteByIds(ids: List<String>)
    suspend fun deleteAll()

    // Comments belong to a post, so they live here rather than in a separate dao. Named
    // insertComments instead of insertAll because the post overload erases to the same JVM
    // signature.
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>
    suspend fun insertComments(comments: List<CommentEntity>)
}
