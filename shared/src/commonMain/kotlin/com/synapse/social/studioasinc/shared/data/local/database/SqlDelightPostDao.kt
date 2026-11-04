package com.synapse.social.studioasinc.shared.data.local.database
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.synapse.social.studioasinc.shared.data.local.entity.CommentEntity
import com.synapse.social.studioasinc.shared.data.local.entity.PostEntity
import com.synapse.social.studioasinc.shared.data.database.Post as DbPost
import com.synapse.social.studioasinc.shared.data.database.Comment as DbComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightPostDao(
    private val db: StorageDatabase
) : PostDao {

    override suspend fun insert(post: PostEntity): Unit = withContext(AppDispatchers.IO) {
        db.postQueries.insertPost(toDbPost(post))
    }

    override suspend fun insertAll(posts: List<PostEntity>) = withContext(AppDispatchers.IO) {
        db.transaction {
            posts.forEach { post ->
                db.postQueries.insertPost(toDbPost(post))
            }
        }
    }

    override suspend fun getPostById(id: String): PostEntity? = withContext(AppDispatchers.IO) {
        db.postQueries.selectById(id).executeAsOneOrNull()?.let { toEntity(it) }
    }

    override suspend fun getAllPosts(): List<PostEntity> = withContext(AppDispatchers.IO) {
        db.postQueries.selectAll().executeAsList().map { toEntity(it) }
    }


    override suspend fun getPostsPaged(limit: Long, offset: Long): List<PostEntity> = withContext(AppDispatchers.IO) {
        db.postQueries.selectAllPaged(limit, offset).executeAsList().map { toEntity(it) }
    }

    override suspend fun getPostsCount(): Long = withContext(AppDispatchers.IO) {
        db.postQueries.countAllPosts().executeAsOne()
    }

    override fun getAllPostsAsFlow(): Flow<List<PostEntity>> {
        return db.postQueries.selectAll()
            .asFlow()
            .mapToList(AppDispatchers.IO)
            .map { list -> list.map { toEntity(it) } }
    }

    override suspend fun getAllPostIds(): List<String> = withContext(AppDispatchers.IO) {
        db.postQueries.selectIds().executeAsList()
    }

    override suspend fun deleteById(id: String): Unit = withContext(AppDispatchers.IO) {
        db.postQueries.deleteById(id)
    }

    override suspend fun deleteByIds(ids: List<String>) = withContext(AppDispatchers.IO) {
        db.transaction {
            ids.forEach { id ->
                db.postQueries.deleteById(id)
            }
        }
    }

    override suspend fun deleteAll(): Unit = withContext(AppDispatchers.IO) {
        db.postQueries.deleteAll()
    }

    override fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> {
        return db.commentQueries.selectByPostId(postId)
            .asFlow()
            .mapToList(AppDispatchers.IO)
            .map { comments -> comments.map { toCommentEntity(it) } }
    }

    override suspend fun insertComments(comments: List<CommentEntity>) = withContext(AppDispatchers.IO) {
        db.transaction {
            comments.forEach { comment ->
                db.commentQueries.insertComment(toDbComment(comment))
            }
        }
    }

    private fun toCommentEntity(dbComment: DbComment): CommentEntity {
        return CommentEntity(
            id = dbComment.id,
            postId = dbComment.postId,
            authorUid = dbComment.authorId,
            text = dbComment.text,
            timestamp = dbComment.timestamp,
            username = dbComment.username,
            avatarUrl = dbComment.avatarUrl,
            parentCommentId = dbComment.parentCommentId,
            linkPreview = dbComment.linkPreview,
            viewsCount = dbComment.viewsCount
        )
    }

    private fun toDbComment(entity: CommentEntity): DbComment {
        return DbComment(
            id = entity.id,
            postId = entity.postId,
            authorId = entity.authorUid,
            text = entity.text,
            timestamp = entity.timestamp,
            likesCount = 0,
            repliesCount = 0,
            isDeleted = false,
            parentCommentId = entity.parentCommentId,
            username = entity.username,
            avatarUrl = entity.avatarUrl,
            linkPreview = entity.linkPreview,
            viewsCount = entity.viewsCount,
            // Sync metadata
            updatedAt = 0L,
            isLocallyDeleted = false,
            lastSyncedAt = null,
            syncVersion = 1
        )
    }

    private fun toEntity(dbPost: DbPost): PostEntity {
        return PostEntity(
            id = dbPost.id,
            key = dbPost.key,
            authorUid = dbPost.authorUid,
            postText = dbPost.postText,
            postImage = dbPost.postImage,
            postType = dbPost.postType,
            postHideViewsCount = dbPost.postHideViewsCount,
            postHideLikeCount = dbPost.postHideLikeCount,
            postHideCommentsCount = dbPost.postHideCommentsCount,
            postDisableComments = dbPost.postDisableComments,
            postVisibility = dbPost.postVisibility,
            publishDate = dbPost.publishDate,
            createdAt = dbPost.createdAt,
            timestamp = dbPost.timestamp,
            likesCount = dbPost.likesCount,
            commentsCount = dbPost.commentsCount,
            viewsCount = dbPost.viewsCount,
            resharesCount = dbPost.resharesCount,
            mediaItems = dbPost.mediaItems,
            linkPreviews = dbPost.linkPreviews,
            isEncrypted = dbPost.isEncrypted,
            nonce = dbPost.nonce,
            encryptionKeyId = dbPost.encryptionKeyId,
            encryptedContent = dbPost.encryptedContent,
            isDeleted = dbPost.isDeleted,
            isEdited = dbPost.isEdited,
            editedAt = dbPost.editedAt,
            deletedAt = dbPost.deletedAt,
            hasPoll = dbPost.hasPoll,
            pollQuestion = dbPost.pollQuestion,
            pollOptions = dbPost.pollOptions,
            pollEndTime = dbPost.pollEndTime,
            pollAllowMultiple = dbPost.pollAllowMultiple,
            hasLocation = dbPost.hasLocation,
            locationName = dbPost.locationName,
            locationAddress = dbPost.locationAddress,
            locationLatitude = dbPost.locationLatitude,
            locationLongitude = dbPost.locationLongitude,
            locationPlaceId = dbPost.locationPlaceId,
            youtubeUrl = dbPost.youtubeUrl,
            reactions = dbPost.reactions,
            userReaction = dbPost.userReaction,
            username = dbPost.username,
            displayName = dbPost.displayName,
            avatarUrl = dbPost.avatarUrl,
            isVerified = dbPost.isVerified,
            userPollVote = dbPost.userPollVote,
            metadata = dbPost.metadata,
            quotedPostId = dbPost.quotedPostId,
            isQuote = dbPost.isQuote,
            rootPostId = dbPost.rootPostId
        )
    }

    private fun toDbPost(entity: PostEntity): DbPost {
        return DbPost(
            id = entity.id,
            key = entity.key,
            authorUid = entity.authorUid,
            postText = entity.postText,
            postImage = entity.postImage,
            postType = entity.postType,
            postHideViewsCount = entity.postHideViewsCount,
            postHideLikeCount = entity.postHideLikeCount,
            postHideCommentsCount = entity.postHideCommentsCount,
            postDisableComments = entity.postDisableComments,
            postVisibility = entity.postVisibility,
            publishDate = entity.publishDate,
            createdAt = entity.createdAt,
            timestamp = entity.timestamp,
            likesCount = entity.likesCount,
            commentsCount = entity.commentsCount,
            viewsCount = entity.viewsCount,
            resharesCount = entity.resharesCount,
            mediaItems = entity.mediaItems,
            linkPreviews = entity.linkPreviews,
            isEncrypted = entity.isEncrypted,
            nonce = entity.nonce,
            encryptionKeyId = entity.encryptionKeyId,
            encryptedContent = entity.encryptedContent,
            isDeleted = entity.isDeleted,
            isEdited = entity.isEdited,
            editedAt = entity.editedAt,
            deletedAt = entity.deletedAt,
            hasPoll = entity.hasPoll,
            pollQuestion = entity.pollQuestion,
            pollOptions = entity.pollOptions,
            pollEndTime = entity.pollEndTime,
            pollAllowMultiple = entity.pollAllowMultiple,
            hasLocation = entity.hasLocation,
            locationName = entity.locationName,
            locationAddress = entity.locationAddress,
            locationLatitude = entity.locationLatitude,
            locationLongitude = entity.locationLongitude,
            locationPlaceId = entity.locationPlaceId,
            youtubeUrl = entity.youtubeUrl,
            reactions = entity.reactions,
            userReaction = entity.userReaction,
            username = entity.username,
            displayName = entity.displayName,
            avatarUrl = entity.avatarUrl,
            isVerified = entity.isVerified,
            userPollVote = entity.userPollVote,
            metadata = entity.metadata,
            quotedPostId = entity.quotedPostId,
            isQuote = entity.isQuote,
            rootPostId = entity.rootPostId,
            // Sync metadata
            updatedAt = 0L,
            isLocallyDeleted = false,
            lastSyncedAt = null,
            syncVersion = 1
        )
    }
}
