package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.Post
import javax.inject.Inject

class QuotePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, text: String): Result<Post> {
        return postRepository.quotePost(postId, text)
    }
}
