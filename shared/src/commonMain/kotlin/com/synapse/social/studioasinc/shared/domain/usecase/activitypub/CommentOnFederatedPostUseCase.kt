package com.synapse.social.studioasinc.shared.domain.usecase.activitypub

import com.synapse.social.studioasinc.shared.domain.repository.IActivityPubRepository

class CommentOnFederatedPostUseCase(private val repository: IActivityPubRepository) {
    suspend operator fun invoke(postId: String, content: String): Result<Unit> {
        return repository.replyToFederatedPost(postId, content)
    }
}
