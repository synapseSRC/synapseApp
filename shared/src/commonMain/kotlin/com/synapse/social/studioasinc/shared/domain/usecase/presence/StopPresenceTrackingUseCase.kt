package com.synapse.social.studioasinc.shared.domain.usecase.presence

import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import io.github.aakira.napier.Napier

class StopPresenceTrackingUseCase(private val repository: PresenceRepository) {
    suspend operator fun invoke() {
        Napier.d("StopPresenceTrackingUseCase invoked")
        repository.stopPresenceTracking()
    }
}
