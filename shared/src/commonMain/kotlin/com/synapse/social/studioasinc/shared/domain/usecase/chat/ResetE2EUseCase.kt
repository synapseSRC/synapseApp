package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository

class ResetE2EUseCase(
    private val repository: ChatRepository,
    private val signalProtocolManager: SignalProtocolManager? = null
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            // 1. Delete all local sessions
            signalProtocolManager?.deleteAllSessions()
            
            // 2. We don't delete local identity here to avoid total loss of history
            // but we could if we wanted a TRUE fresh start.
            // For now, let's just trigger a re-initialization which will check mismatch.
            
            // 3. Re-initialize (which will re-upload keys if mismatch, or we can force it)
            repository.initializeE2EE()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
