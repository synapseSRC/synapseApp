package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.Post
import javax.inject.Inject

class GetPostUseCase @Inject constructor(private val postRepository: PostRepository) {
    suspend operator fun invoke(postId: String): Result<Post?> {
        return postRepository.getPost(postId)
    }
}
