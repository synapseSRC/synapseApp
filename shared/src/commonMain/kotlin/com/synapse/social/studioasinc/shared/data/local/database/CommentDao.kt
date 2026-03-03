package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

interface CommentDao {
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>>
    suspend fun insertAll(comments: List<CommentEntity>)
}
