package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.model.presence.UserPresence
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserPresenceDetailUseCase(
    private val repository: PresenceRepository
) {
    operator fun invoke(userId: String): Flow<UserPresence> = repository.observeUserPresenceDetail(userId)
}
