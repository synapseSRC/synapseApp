package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.Post
import javax.inject.Inject

class GetPostByIdUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postId: String): Result<Post?> {
        return repository.getPost(postId)
    }
}
