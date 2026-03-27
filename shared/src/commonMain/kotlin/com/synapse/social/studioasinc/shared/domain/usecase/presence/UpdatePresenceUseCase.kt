package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository

class UpdatePresenceUseCase(private val repository: PresenceRepository) {
    suspend operator fun invoke(isOnline: Boolean) = repository.updatePresence(isOnline)
}
