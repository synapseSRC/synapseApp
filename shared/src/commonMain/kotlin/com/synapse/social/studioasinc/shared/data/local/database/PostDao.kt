package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

interface PostDao {
    suspend fun insert(post: PostEntity)
    suspend fun insertAll(posts: List<PostEntity>)
    suspend fun getPostById(id: String): PostEntity?
    suspend fun getAllPosts(): List<PostEntity>
    fun getAllPostsAsFlow(): Flow<List<PostEntity>>
    suspend fun getAllPostIds(): List<String>
    suspend fun deleteById(id: String)
    suspend fun deleteByIds(ids: List<String>)
    suspend fun deleteAll()
}
