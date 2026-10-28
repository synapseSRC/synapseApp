package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository

/**
 * Asks the backend to close calls that stopped making progress.
 *
 * Run before reading history: a call that died with the app is still an open row until
 * the server is told to look at it, and an open row would otherwise be missing from the
 * list the user is about to see.
 */
class ResolveStaleCallSessionsUseCase(
    private val sessions: CallSessionRepository
) {
    suspend operator fun invoke(): Result<Int> = sessions.resolveStaleSessions()
}
