package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Says no to a call ringing here.
 *
 * Rejecting is still allowed once the call has moved to connecting: the peer may have tapped accept a
 * half-second before this device finished waking up, and a user who changes their mind in that window
 * is hanging up, not leaving an unanswered ring behind.
 */
class DeclineCallUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession): Result<Unit> {
        session.endedRejection("decline the call")?.let { return Result.failure(it) }
        if (session.direction != CallDirection.INCOMING) {
            return Result.failure(IllegalStateException("Only the person being called can decline it"))
        }
        val actionable = session.phase == CallPhase.RINGING || session.phase == CallPhase.CONNECTING
        if (!actionable) {
            return Result.failure(IllegalStateException("This call is no longer waiting for an answer"))
        }
        return callRepository.decline(session.id)
    }
}
