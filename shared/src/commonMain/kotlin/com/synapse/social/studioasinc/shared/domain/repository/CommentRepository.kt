package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.Comment

interface CommentRepository {
    suspend fun getComments(postId: String, parentId: String? = null): Result<List<Comment>>
    suspend fun addComment(postId: String, content: String, parentId: String?, mediaUrl: String? = null): Result<Comment>
    suspend fun deleteComment(commentId: String): Result<Unit>
}
