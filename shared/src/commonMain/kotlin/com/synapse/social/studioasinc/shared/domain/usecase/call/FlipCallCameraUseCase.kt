package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Swap between this device's front and rear camera.
 *
 * Deliberately not a toggle: there is no "which lens is next" to express, because a phone with one
 * camera would flip to itself. The repository asks the capturer for the *other* lens and reports the
 * result back through the session, so an unavailable rear camera leaves the state honest instead of
 * showing a flipped camera that never flipped.
 */
class FlipCallCameraUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession): Result<Unit> {
        session.endedRejection("switch the camera")?.let { return Result.failure(it) }
        if (session.mediaType != CallMediaType.VIDEO) {
            return Result.failure(IllegalStateException("This call has no camera to switch"))
        }
        return callRepository.flipCamera(session.id)
    }
}
