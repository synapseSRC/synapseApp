package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.IncomingCallWatcher
import kotlinx.coroutines.flow.Flow

/**
 * The invitation the app should surface right now, if any.
 *
 * A cold `null` means there is nothing to show, and it is as meaningful as a session: it is what stops
 * a ringing screen from outliving the ring. The policy behind it — which rows count as a ring this
 * device has not already surfaced — belongs to the watcher, not to the screen, so a call arriving over
 * push, over realtime or after a process restart all reach the same place with the same meaning.
 */
class ObserveIncomingCallsUseCase(
    private val watcher: IncomingCallWatcher
) {
    operator fun invoke(): Flow<CallSession?> = watcher.incomingCall
}
