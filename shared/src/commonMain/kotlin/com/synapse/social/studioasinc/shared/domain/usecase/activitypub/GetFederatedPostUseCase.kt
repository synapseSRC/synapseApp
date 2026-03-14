package com.synapse.social.studioasinc.shared.domain.usecase.activitypub

import com.synapse.social.studioasinc.shared.domain.model.activitypub.ActivityPubObject
import com.synapse.social.studioasinc.shared.domain.repository.IActivityPubRepository

class GetFederatedPostUseCase(private val repository: IActivityPubRepository) {
    suspend operator fun invoke(postId: String): Result<ActivityPubObject> {
        return repository.getFederatedPost(postId)
    }
}
