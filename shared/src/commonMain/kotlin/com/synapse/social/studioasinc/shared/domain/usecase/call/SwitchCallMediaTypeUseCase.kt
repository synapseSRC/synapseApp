package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaChange
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallRequest
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository

/**
 * Changes the media of a call, and says honestly whether that means renegotiating or re-dialling.
 *
 * The distinction is the whole reason this is a UseCase instead of one repository call. Before the
 * two devices are connected there is no session to renegotiate, so asking for video on a call that is
 * still ringing means *placing that call again as a video call*. Doing it in place would either
 * silently no-op — the user taps video and nothing happens — or leave the original ring in the
 * backend as a ghost nobody will ever answer.
 *
 * [CallMediaChange.Redial] is returned *after* the live ring has been cancelled, so the caller's
 * re-dial never races its own abandoned invitation.
 */
class SwitchCallMediaTypeUseCase(
    private val callRepository: CallRepository
) {
    suspend operator fun invoke(session: CallSession, mediaType: CallMediaType): Result<CallMediaChange> {
        if (session.phase == CallPhase.ENDED) {
            return Result.failure(IllegalStateException("The call has already ended"))
        }
        if (session.mediaType == mediaType) {
            return Result.success(CallMediaChange.Renegotiated(mediaType))
        }

        val dialingOut = session.direction == CallDirection.OUTGOING &&
            (session.phase == CallPhase.DIALING || session.phase == CallPhase.RINGING)
        if (!dialingOut) {
            return callRepository.switchMediaType(session.id, mediaType)
                .map { CallMediaChange.Renegotiated(mediaType) }
        }

        return callRepository.end(session.id)
            .map {
                CallMediaChange.Redial(
                    CallRequest(
                        participant = session.participant,
                        chatId = session.chatId,
                        direction = session.direction,
                        mediaType = mediaType
                    )
                )
            }
    }
}
