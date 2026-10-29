package com.synapse.social.studioasinc.shared.domain.model.sound

/**
 * Which user choice governs a cue.
 */
enum class SoundToneCategory {
    MESSAGE,
    CALL
}

/**
 * An audio cue, named for the event it confirms rather than for the file that carries it.
 *
 * Several clips come from a pack whose vocabulary describes a group voice channel — something this app
 * does not have. The intent names here keep that vocabulary inside the platform implementation, so a
 * later clip swap never reaches the domain.
 */
enum class SoundEffect(val category: SoundToneCategory) {
    // Message tones
    MessageIncoming(SoundToneCategory.MESSAGE),
    MessageOutgoing(SoundToneCategory.MESSAGE),
    RecordingStarted(SoundToneCategory.MESSAGE),

    // Call tones
    CallRingbackLoop(SoundToneCategory.CALL),
    CallConnecting(SoundToneCategory.CALL),
    CallReconnecting(SoundToneCategory.CALL),
    CallRemoteAnswered(SoundToneCategory.CALL),
    CallRemoteEnded(SoundToneCategory.CALL),
    CallEnded(SoundToneCategory.CALL),
    CallBusy(SoundToneCategory.CALL),
    CallFailed(SoundToneCategory.CALL),
    CallUnmuteConfirmed(SoundToneCategory.CALL)
}

/**
 * How long a cue plays for.
 *
 * [STOP] ends a [LOOP] and is deliberately *not* subject to the user's sound settings: a cue that was
 * started while sound was on must still be able to be silenced after the user turns sound off.
 */
enum class SoundPlaybackMode {
    ONCE,
    LOOP,
    STOP
}

/**
 * One thing to put on the audio route, as decided for a single transition.
 *
 * A transition can need both halves at once — the peer answering means the local ringback must stop
 * *and* the connecting cue must start — so a cue is a list, not one value.
 */
data class SoundRequest(
    val effect: SoundEffect,
    val mode: SoundPlaybackMode
)

data class SoundPreferences(
    val masterEnabled: Boolean = true,
    val messageTonesEnabled: Boolean = true,
    val callTonesEnabled: Boolean = true
) {
    /** A category switch is only ever an exception carved out of the master switch. */
    fun isCategoryEnabled(category: SoundToneCategory): Boolean =
        masterEnabled && when (category) {
            SoundToneCategory.MESSAGE -> messageTonesEnabled
            SoundToneCategory.CALL -> callTonesEnabled
        }
}
