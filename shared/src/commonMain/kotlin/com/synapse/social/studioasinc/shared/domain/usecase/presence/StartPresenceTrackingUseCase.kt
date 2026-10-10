package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository

class StartPresenceTrackingUseCase(private val repository: PresenceRepository) {
    suspend operator fun invoke() = repository.startPresenceTracking()
}
