package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Hangs up.
 *
 * Ending a call that is already over is reported as success rather than as an error, because the only
 * caller that can hit it is a screen closing itself: the peer may have ended the call first, in which
 * case what the user asked for — a call that is not happening — is already true, and surfacing a
 * failure would show an error for a state the user wanted.
 */
class EndCallUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession?): Result<Unit> {
        if (session == null) return Result.failure(IllegalStateException("No call to end"))
        if (session.phase == CallPhase.ENDED) return Result.success(Unit)
        return callRepository.end(session.id)
    }
}
