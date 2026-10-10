package com.synapse.social.studioasinc.shared.domain.usecase.comment

import com.synapse.social.studioasinc.shared.domain.model.Comment
import com.synapse.social.studioasinc.shared.domain.repository.CommentRepository

class GetCommentsUseCase(private val repository: CommentRepository) {
    suspend operator fun invoke(postId: String, parentId: String? = null): Result<List<Comment>> {
        return repository.getComments(postId, parentId)
    }
}
