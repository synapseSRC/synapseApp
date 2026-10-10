package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserPresenceUseCase(private val repository: PresenceRepository) {
    operator fun invoke(userId: String): Flow<Boolean> = repository.observeUserPresence(userId)
}
