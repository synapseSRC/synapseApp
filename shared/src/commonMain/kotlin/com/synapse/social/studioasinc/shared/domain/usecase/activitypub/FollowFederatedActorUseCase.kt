package com.synapse.social.studioasinc.shared.domain.usecase.activitypub

import com.synapse.social.studioasinc.shared.domain.repository.IActivityPubRepository

class FollowFederatedActorUseCase(private val repository: IActivityPubRepository) {
    suspend operator fun invoke(actorId: String): Result<Unit> {
        return repository.followFederatedActor(actorId)
    }
}
