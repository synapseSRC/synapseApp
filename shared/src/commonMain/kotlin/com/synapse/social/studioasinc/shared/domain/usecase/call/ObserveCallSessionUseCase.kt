package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import kotlinx.coroutines.flow.Flow

/**
 * The single stream the call screen reads.
 *
 * Every phase on it was decided by the backend plus this device's own link, which is why the UI is
 * allowed to render it directly instead of interpreting it: nothing here is a guess about what the
 * other person is doing, and nothing is a local timer dressed up as progress.
 */
class ObserveCallSessionUseCase(
    private val callRepository: CallRepository
) {
    operator fun invoke(callId: String): Flow<CallSession?> = callRepository.observeSession(callId)
}
