package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository

/**
 * Keeps a connected call from being mistaken for an abandoned one.
 *
 * The backend closes a live session once its last beat is older than the stale window,
 * so a call keeps beating for as long as media is up and stops the moment it tears down.
 */
class CallHeartbeatUseCase(
    private val sessions: CallSessionRepository
) {
    suspend operator fun invoke(callId: String): Result<Unit> = sessions.heartbeat(callId)
}
