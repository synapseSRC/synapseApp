package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.activitypub.ActivityPubActor
import com.synapse.social.studioasinc.shared.domain.model.activitypub.ActivityPubObject

interface IActivityPubRepository {
    suspend fun searchFederatedActors(query: String): Result<List<ActivityPubActor>>
    suspend fun getFederatedPost(postId: String): Result<ActivityPubObject>
    suspend fun followFederatedActor(actorId: String): Result<Unit>
    suspend fun replyToFederatedPost(postId: String, content: String): Result<Unit>
}
