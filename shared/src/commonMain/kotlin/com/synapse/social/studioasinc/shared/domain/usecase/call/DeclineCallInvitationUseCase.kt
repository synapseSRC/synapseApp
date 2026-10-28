package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository

/**
 * Says no to a ring that this device never opened a screen for.
 *
 * [DeclineCallUseCase] is the in-call path: it refuses when the session has ended, when the caller is
 * asking about somebody else's call, and it tears the local media down through `CallRepository`. That
 * path needs a live session, and a decline from the notification shade has none — the app may have been
 * closed when the call came in, so nothing here was ever capturing audio. This UseCase writes the
 * transition to the backend and stops, which is the whole of what a ring without a screen can mean:
 * the caller's device learns the answer from the row, and a screen that does exist picks the same row
 * up from realtime and closes itself.
 *
 * The backend is still the authority on who may decline. A call id that is not this user's ring is
 * refused by row-level security, not by a check here.
 */
class DeclineCallInvitationUseCase(
    private val sessions: CallSessionRepository
) {
    suspend operator fun invoke(callId: String): Result<CallSession> {
        if (callId.isBlank()) {
            return Result.failure(IllegalStateException("There is no call to decline"))
        }
        return sessions.decline(callId)
    }
}
