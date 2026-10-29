package com.synapse.social.studioasinc.data.call.audio

import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the phone's audio path for the duration of a call.
 *
 * The repository asks, this answers with what the device *actually* did. That distinction is the
 * whole point: a user tapping "speaker" on a phone that is mid-SCO-connection needs the icon to
 * show reality, not the intent, and the call UI can only do that if the layer below refuses to
 * echo the request back as a success.
 */
interface CallAudioRouter {

    /**
     * Switches the system into call mode and picks the sensible starting device.
     *
     * [video] exists because a video call is watched by more than one person in the room and
     * defaults to the loudspeaker; a voice call defaults to the earpiece unless something better is
     * already plugged in. Returns the route the device settled on.
     */
    fun start(video: Boolean): CallAudioRoute

    /** The route in use, re-emitted whenever the system plugs or unplugs something. */
    val activeRoute: StateFlow<CallAudioRoute>

    /**
     * Applies [route] and returns the route the hardware reports afterwards, which may differ —
     * asking for Bluetooth with no paired headset is a legitimate request that simply cannot be
     * honoured, and the caller must be able to tell.
     */
    fun select(route: CallAudioRoute): CallAudioRoute

    /** Restores whatever the audio stack was doing before the call. Idempotent. */
    fun stop()
}
