package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.data.call.webrtc.EngineConnectionState
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendStatus
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

/**
 * The one place that decides which call phase the UI is allowed to show.
 *
 * Two inputs feed it, and neither one alone is enough:
 *
 *  - the row the **backend** wrote, which proves the other person is genuinely in this call;
 *  - what our own **ICE agent** observed, which proves media is actually flowing.
 *
 * [CallPhase.ACTIVE] requires both. That single rule is why no timer, retry loop, cached row or
 * mis-timed response can put a connected call on screen: the local engine alone can only ever
 * reach [CallPhase.CONNECTING], and the backend alone cannot say the audio arrived.
 *
 * The debug simulator runs through the same table, so a simulated state has to be a legal state,
 * which keeps the debug sheet honest about what the production UI is allowed to render.
 */
object CallStateMachine {

    /**
     * Legal forward moves. Nothing goes backwards: a call that reached ACTIVE never becomes
     * RINGING again, and [CallPhase.ENDED] is terminal, because the UI keys its teardown off it.
     */
    private val legalMoves: Map<CallPhase, Set<CallPhase>> = mapOf(
        CallPhase.DIALING to setOf(CallPhase.RINGING, CallPhase.CONNECTING, CallPhase.ACTIVE, CallPhase.ENDED),
        CallPhase.RINGING to setOf(CallPhase.CONNECTING, CallPhase.ACTIVE, CallPhase.ENDED),
        CallPhase.CONNECTING to setOf(CallPhase.ACTIVE, CallPhase.RECONNECTING, CallPhase.ENDED),
        CallPhase.ACTIVE to setOf(CallPhase.RECONNECTING, CallPhase.ENDED),
        CallPhase.RECONNECTING to setOf(CallPhase.ACTIVE, CallPhase.ENDED),
        CallPhase.ENDED to emptySet()
    )

    fun canMove(from: CallPhase, to: CallPhase): Boolean = to in legalMoves[from].orEmpty()

    /**
     * Applies [to] to [session], stamping the timestamps that belong to the phases they describe.
     * An illegal move is dropped rather than thrown: a late event from a call that already ended
     * is normal, and crashing on it would turn a harmless race into a lost call screen.
     */
    fun move(
        session: CallSession,
        to: CallPhase,
        nowMillis: Long = System.currentTimeMillis()
    ): CallSession {
        if (session.phase == to) return session
        if (!canMove(session.phase, to)) return session
        return when (to) {
            CallPhase.ACTIVE -> session.copy(
                phase = to,
                connectedAtMillis = session.connectedAtMillis ?: nowMillis
            )

            CallPhase.ENDED -> session.copy(
                phase = to,
                endedAtMillis = session.endedAtMillis ?: nowMillis
            )

            else -> session.copy(phase = to)
        }
    }

    /**
     * Resolves the phase from what is known, which is the whole anti-simulation rule in one function.
     *
     * [link] is null until this device has an engine reporting something: "not proven" is deliberately
     * distinct from [EngineConnectionState.CONNECTED], and reads as CONNECTING rather than as a pass.
     */
    fun phaseFor(
        status: CallBackendStatus?,
        direction: CallDirection,
        link: EngineConnectionState?
    ): CallPhase = when (status) {
        // No row yet. Only the caller can be in that window — it is dialling while the backend
        // writes the invitation — and an incoming call without a row is a UI with nothing to show.
        null -> if (direction == CallDirection.INCOMING) CallPhase.RINGING else CallPhase.DIALING

        CallBackendStatus.ENDED -> CallPhase.ENDED

        // The peer has been told, nobody has picked up yet. The callee sees the same word for its
        // own ringing, which is accurate on both sides.
        CallBackendStatus.RINGING -> CallPhase.RINGING

        CallBackendStatus.ANSWERED ->
            if (link == EngineConnectionState.CONNECTED) CallPhase.ACTIVE else CallPhase.CONNECTING

        // The server asserts the call is up; our own ICE agent still gets the last word on whether
        // this device can hear it.
        CallBackendStatus.ACTIVE -> when (link) {
            EngineConnectionState.CONNECTED -> CallPhase.ACTIVE
            EngineConnectionState.NEW, EngineConnectionState.CLOSED, null -> CallPhase.CONNECTING
            EngineConnectionState.CONNECTING,
            EngineConnectionState.DISCONNECTED,
            EngineConnectionState.FAILED -> CallPhase.RECONNECTING
        }

        CallBackendStatus.RECONNECTING ->
            if (link == EngineConnectionState.CONNECTED) CallPhase.ACTIVE else CallPhase.RECONNECTING
    }

    /** True for the phases where a media link is expected to exist. */
    fun isLiveConnection(phase: CallPhase): Boolean =
        phase == CallPhase.CONNECTING || phase == CallPhase.ACTIVE || phase == CallPhase.RECONNECTING

    fun isSettled(phase: CallPhase): Boolean = phase == CallPhase.ENDED

    /**
     * What this device may *claim* the call ended as.
     *
     * A claim, not a verdict: `end_call` re-reads the row and overrides it when its own view of the
     * phase disagrees, so a client that lies about a ring nobody answered still gets logged honestly.
     */
    fun claimEndReason(session: CallSession): CallEndReason = when {
        session.connectedAtMillis != null -> CallEndReason.COMPLETED
        session.direction == CallDirection.INCOMING -> CallEndReason.DECLINED
        else -> CallEndReason.CANCELLED
    }
}
