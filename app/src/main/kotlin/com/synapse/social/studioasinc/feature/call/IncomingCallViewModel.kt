package com.synapse.social.studioasinc.feature.call

import androidx.lifecycle.ViewModel
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.usecase.call.ObserveIncomingCallsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * The host's ears for calls addressed to this user.
 *
 * Not a screen's ViewModel: nothing here renders. It exists because the app as a whole has to notice a
 * ring, and only a screen-less owner can do that without pretending to be the call screen — the screen
 * that rings is opened *from* this, through [CallLaunchRequest], and takes over from there.
 *
 * The one rule it adds over the UseCase is that an invitation is offered **once per call**. Without it, a
 * user who backs out of a ringing screen would be walked straight back into it, since the backend is
 * still waiting for an answer and the watch still holds the row. That memory dies with this ViewModel,
 * which is the right lifetime: a fresh app start should be able to surface a call that is still ringing.
 */
@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    observeIncomingCalls: ObserveIncomingCallsUseCase
) : ViewModel() {

    private val surfacedIds = mutableSetOf<String>()

    /** Each invitation this app has not already been offered, in the order it arrived. */
    val newInvitation: Flow<CallLaunchRequest> = observeIncomingCalls()
        .filterNotNull()
        .distinctUntilChangedBy(CallSession::id)
        .filter { surfacedIds.add(it.id) }
        .map(CallSession::toIncomingLaunchRequest)
}
