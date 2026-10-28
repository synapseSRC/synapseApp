package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Covers or uncovers this device's camera.
 *
 * The media type is checked rather than inferred: an audio call has no video track to disable, and
 * asking the engine for one is the kind of failure that would otherwise arrive as a silent no-op on a
 * button the user can see.
 */
class ToggleLocalVideoUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession): Result<Unit> {
        session.endedRejection("change the camera")?.let { return Result.failure(it) }
        if (session.mediaType != CallMediaType.VIDEO) {
            return Result.failure(IllegalStateException("This call has no camera to switch"))
        }
        return callRepository.setLocalVideoEnabled(session.id, !session.isLocalVideoEnabled)
    }
}
