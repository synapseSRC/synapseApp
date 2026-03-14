package com.synapse.social.studioasinc.shared.domain.usecase.activitypub

import com.synapse.social.studioasinc.shared.domain.model.activitypub.ActivityPubActor
import com.synapse.social.studioasinc.shared.domain.repository.IActivityPubRepository

class SearchFederatedContentUseCase(private val repository: IActivityPubRepository) {
    suspend operator fun invoke(query: String): Result<List<ActivityPubActor>> {
        return repository.searchFederatedActors(query)
    }
}
