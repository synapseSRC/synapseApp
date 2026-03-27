package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.data.database.Comment
import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class SqlDelightCommentDao(
    private val db: StorageDatabase
) : CommentDao {

    override fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> {
        return db.commentQueries.selectByPostId(postId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { comments ->
                comments.map { it.toEntity() }
            }
    }

    override suspend fun insertAll(comments: List<CommentEntity>) {
        db.transaction {
            comments.forEach { comment ->
                db.commentQueries.insertComment(comment.toModel())
            }
        }
    }

    private fun Comment.toEntity(): CommentEntity {
        return CommentEntity(
            id = id,
            postId = postId,
            authorUid = authorId,
            text = text,
            timestamp = timestamp,
            username = username,
            avatarUrl = avatarUrl,
            parentCommentId = parentCommentId
        )
    }

    private fun CommentEntity.toModel(): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorId = authorUid,
            text = text,
            timestamp = timestamp,
            likesCount = 0,
            repliesCount = 0,
            isDeleted = false,
            parentCommentId = parentCommentId,
            username = username,
            avatarUrl = avatarUrl
        )
    }
}
