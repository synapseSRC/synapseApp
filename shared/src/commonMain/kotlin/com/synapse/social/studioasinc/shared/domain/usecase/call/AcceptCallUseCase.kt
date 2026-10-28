package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Answers a call that is ringing on this device.
 *
 * The caller can never reach this: `accept_call` on the backend also refuses anyone who is not the
 * callee, so these checks are what keeps a tap that cannot work from looking like one that did.
 * Only a call still ringing can be accepted, because a ring that already moved on has an answer, a
 * decline or an expiry recorded against it.
 */
class AcceptCallUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession): Result<Unit> {
        session.endedRejection("accept the call")?.let { return Result.failure(it) }
        if (session.direction != CallDirection.INCOMING) {
            return Result.failure(IllegalStateException("Only the person being called can accept it"))
        }
        if (session.phase != CallPhase.RINGING) {
            return Result.failure(IllegalStateException("This call is no longer waiting for an answer"))
        }
        return callRepository.accept(session.id)
    }
}
