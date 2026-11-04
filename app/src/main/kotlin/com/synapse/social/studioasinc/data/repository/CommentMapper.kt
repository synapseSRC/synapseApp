package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import com.synapse.social.studioasinc.domain.model.Comment
import com.synapse.social.studioasinc.shared.data.database.Comment as DbComment

// The legacy `Comment` domain model is still the currency of the SQLDelight comment cache and is
// bridged to/from `CommentWithUser` here. Retiring it means reworking that cache path onto the Post
// model — a data-layer migration, not a warning fix — hence the documented suppression.
@Suppress("DEPRECATION")
object CommentMapper {

    fun toSharedEntity(comment: Comment, username: String? = null, avatarUrl: String? = null): CommentEntity {
        return CommentEntity(
            id = comment.key,
            postId = comment.postKey,
            authorUid = comment.uid,
            text = comment.comment,
            timestamp = parsePushTime(comment.pushTime),
            parentCommentId = comment.replyCommentKey,
            username = username,
            avatarUrl = avatarUrl
        )
    }

    fun toModel(entity: CommentEntity): Comment {
        return Comment(
            key = entity.id,
            postKey = entity.postId,
            uid = entity.authorUid,
            comment = entity.text,
            pushTime = entity.timestamp.toString(),
            replyCommentKey = entity.parentCommentId
        )
    }

    fun toModel(entity: DbComment): Comment {
         return Comment(
            key = entity.id,
            postKey = entity.postId,
            uid = entity.authorId,
            comment = entity.text,
            pushTime = entity.timestamp.toString(),
            replyCommentKey = entity.parentCommentId
        )
    }

    private fun parsePushTime(pushTime: String): Long {
        return try {
            pushTime.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
