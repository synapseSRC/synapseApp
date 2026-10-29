package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Flips mute, rather than setting it.
 *
 * The next value is derived from the session the UI is holding, so a double tap cannot send two
 * contradictory commands, and the repository only ever receives the state the other person is about
 * to hear. A refusal from the repository is not smoothed over: if the microphone did not change, the
 * icon must not either.
 */
class ToggleCallMuteUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession): Result<Unit> {
        session.endedRejection("change the microphone")?.let { return Result.failure(it) }
        return callRepository.setMuted(session.id, !session.isMuted)
    }
}
