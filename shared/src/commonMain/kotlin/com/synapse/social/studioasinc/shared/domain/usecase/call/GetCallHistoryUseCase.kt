package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository

/**
 * Ended calls the signed-in user took part in, newest first.
 *
 * The Calls tab reads this rather than a chat-scoped list: a call belongs to the person
 * who made or took it, and a session's chat is an implementation detail of how the
 * backend attributes it.
 */
class GetCallHistoryUseCase(
    private val sessions: CallSessionRepository
) {
    suspend operator fun invoke(limit: Int = 30): Result<List<CallSession>> =
        sessions.getCallHistory(limit)
}
