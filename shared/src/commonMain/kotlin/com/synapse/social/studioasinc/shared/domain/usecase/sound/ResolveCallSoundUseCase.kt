package com.synapse.social.studioasinc.shared.domain.usecase.sound

import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPlaybackMode
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundRequest

/**
 * Turns one step of the call state machine into the cues that step deserves.
 *
 * Reads a *transition* — the previous frame and the new one — because a phase name on its own is not
 * enough. `ENDED` says the call is over but not whose doing, and the difference between the other side
 * hanging up, declining, and never answering is the whole reason three separate tones exist.
 *
 * Exactly one closing cue per call, by construction: the end reasons are one `when`, so no combination
 * can fire `CallRemoteEnded` and `CallEnded` for the same hang-up.
 */
class ResolveCallSoundUseCase {

    operator fun invoke(previous: CallSession?, current: CallSession?): List<SoundRequest> {
        val requests = mutableListOf<SoundRequest>()

        // Ringback is the only cue that has to keep going, so its lifecycle is decided on every frame
        // rather than only on a phase change: a replaced session id, or the session disappearing, must
        // still be able to stop it.
        val wasRingingOut = previous.isWaitingForAnswer()
        val isRingingOut = current.isWaitingForAnswer()
        if (isRingingOut && !wasRingingOut) requests += SoundRequest(SoundEffect.CallRingbackLoop, SoundPlaybackMode.LOOP)
        if (wasRingingOut && !isRingingOut) requests += SoundRequest(SoundEffect.CallRingbackLoop, SoundPlaybackMode.STOP)

        if (current == null) return requests

        if (previous?.phase != current.phase) {
            val cue = when (current.phase) {
                // Nobody has committed to anything yet: the caller's own ringback is the only sound here,
                // and it is handled above.
                CallPhase.DIALING,
                CallPhase.RINGING -> null

                CallPhase.CONNECTING -> SoundEffect.CallConnecting

                // Coming back from a dropped connection is not somebody new joining; it says so with its
                // own tone, so replaying the joined cue here would claim a second participant.
                CallPhase.ACTIVE ->
                    if (previous?.phase == CallPhase.RECONNECTING) null else SoundEffect.CallRemoteAnswered

                CallPhase.RECONNECTING -> SoundEffect.CallReconnecting

                CallPhase.ENDED -> current.closingCue()
            }
            cue?.let { requests += SoundRequest(it, SoundPlaybackMode.ONCE) }
        }

        // Read back from the session rather than from the tap, so a control the backend refused cannot
        // announce itself as done. Same id, because a replaced session starts unmuted as its own call.
        if (previous != null && previous.id == current.id && previous.isMuted && !current.isMuted) {
            requests += SoundRequest(SoundEffect.CallUnmuteConfirmed, SoundPlaybackMode.ONCE)
        }

        return requests
    }

    private fun CallSession.closingCue(): SoundEffect = when {
        errorKind == CallErrorKind.BUSY -> SoundEffect.CallBusy
        endReason == CallEndReason.REMOTE_ENDED -> SoundEffect.CallRemoteEnded
        endReason == CallEndReason.DECLINED -> SoundEffect.CallBusy
        endReason == CallEndReason.NO_ANSWER ||
            endReason == CallEndReason.UNAVAILABLE ||
            endReason == CallEndReason.FAILED -> SoundEffect.CallFailed

        // A talk that finished, or a ring this device took back — the call was there and is now not.
        else -> SoundEffect.CallEnded
    }

    private fun CallSession?.isWaitingForAnswer(): Boolean =
        this != null && direction == CallDirection.OUTGOING &&
            (phase == CallPhase.DIALING || phase == CallPhase.RINGING)
}
