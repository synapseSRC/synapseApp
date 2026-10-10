package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.CommentDto
import com.synapse.social.studioasinc.shared.domain.model.Comment
import kotlinx.datetime.Instant

object CommentMapper {
    fun toDomain(dto: CommentDto): Comment {
        return Comment(
            id = dto.id,
            postId = dto.postId,
            authorId = dto.authorId,
            text = if (dto.isDeleted) "This comment was deleted" else dto.content,
            timestamp = try {
                Instant.parse(dto.createdAt).toEpochMilliseconds()
            } catch (e: Exception) {
                0L
            },
            likesCount = dto.likesCount,
            repliesCount = dto.repliesCount,
            parentCommentId = dto.parentId,
            isDeleted = dto.isDeleted,
            username = dto.author?.username,
            avatarUrl = dto.author?.avatarUrl
        )
    }

    fun toDomainList(dtos: List<CommentDto>): List<Comment> = dtos.map { toDomain(it) }
}
