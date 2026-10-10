package com.synapse.social.studioasinc.data.source

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.local.database.CommentDao
import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import com.synapse.social.studioasinc.shared.data.database.Comment as DbComment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CommentLocalDataSource @Inject constructor(
    private val storageDatabase: StorageDatabase,
    private val commentDao: CommentDao
) {

    fun getCommentsByPostIdFromSqlDelight(postId: String): List<DbComment> {
        return storageDatabase.commentQueries.selectByPostId(postId).executeAsList()
    }

    fun getCommentsForPostFlow(postId: String): Flow<List<CommentEntity>> {
        return commentDao.getCommentsForPost(postId)
    }

    suspend fun insertAll(comments: List<CommentEntity>) {
        commentDao.insertAll(comments)
    }

    fun deleteByIdFromSqlDelight(commentId: String) {
        storageDatabase.commentQueries.deleteById(commentId)
    }
}
