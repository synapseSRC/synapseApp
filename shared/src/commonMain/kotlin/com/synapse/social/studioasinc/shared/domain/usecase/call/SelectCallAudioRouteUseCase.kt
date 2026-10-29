package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Moves the call to another output.
 *
 * Repeating the current route is a no-op rather than an error, because the route on screen may be the
 * one the hardware chose by itself — tap the speaker while the call is already on it and there is
 * nothing to change, and nothing should be reported as broken.
 */
class SelectCallAudioRouteUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession, route: CallAudioRoute): Result<Unit> {
        session.endedRejection("change the speaker")?.let { return Result.failure(it) }
        if (session.audioRoute == route) return Result.success(Unit)
        return callRepository.setAudioRoute(session.id, route)
    }
}
