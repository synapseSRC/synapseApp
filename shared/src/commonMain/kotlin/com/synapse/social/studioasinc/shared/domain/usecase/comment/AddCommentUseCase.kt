package com.synapse.social.studioasinc.shared.domain.usecase.comment

import com.synapse.social.studioasinc.shared.domain.model.Comment
import com.synapse.social.studioasinc.shared.domain.repository.CommentRepository

class AddCommentUseCase(private val repository: CommentRepository) {
    suspend operator fun invoke(postId: String, content: String, parentId: String?, mediaUrl: String? = null): Result<Comment> {
        return repository.addComment(postId, content, parentId, mediaUrl)
    }
}
