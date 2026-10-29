package com.synapse.social.studioasinc.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPlaybackMode
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundToneCategory
import com.synapse.social.studioasinc.shared.domain.service.SoundPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the app's sound-effect clips through two [SoundPool]s.
 *
 * Two pools, not one, because a SoundPool's audio usage is fixed when it is built and the two families of
 * cue need different ones: a message blip belongs to the notification stream, so the OS sets its volume
 * with notification volume and DND can silence it, while every call cue belongs to the ringer stream, so
 * it follows the ringer and ducks whatever media is playing. One pool could only be one of those.
 *
 * Nothing here plays on the voice-communication stream. While a call is up, the audio router puts the
 * device in communication mode, and a cue placed on that stream is mixed into the call itself — the peer
 * would hear this device's interface. [setInCallAudioMode] closes the remaining half of that gap by
 * muting the notification cues for the duration of a call, because a blip that comes out of the earpiece
 * is close enough to the microphone to be re-captured outside the echo-cancellation reference.
 *
 * Nothing here requests audio focus, loops included. The only loop is a caller's own ringback, and it lives
 * inside a call this same process already holds `AUDIOFOCUS_GAIN` for: measured on device, the loop's
 * transient request was followed 3ms later by the call router's own request, which came back to us as
 * `handleLoss` — a focus listener then stopped the ring before it was ever audible.
 */
@Singleton
class AndroidSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) : SoundPlayer {

    private val uiPool = buildPool(AudioAttributes.USAGE_NOTIFICATION, MAX_UI_STREAMS)
    private val ringPool = buildPool(AudioAttributes.USAGE_NOTIFICATION_RINGTONE, MAX_RING_STREAMS)

    private val soundIds: Map<SoundEffect, Int> = SoundEffect.entries.associateWith { effect ->
        poolFor(effect.category).load(context, rawIdOf(effect), PLAYBACK_PRIORITY)
    }

    private val activeLoops = mutableMapOf<SoundEffect, Int>()

    @Volatile
    private var inCallAudioMode: Boolean = false

    /**
     * Marks that the device is inside a call's audio route.
     *
     * Called by the call audio router, which is the one thing that both sets and clears that mode, so the
     * flag cannot outlive a call. Call cues keep playing: they are the subject of the call.
     */
    fun setInCallAudioMode(active: Boolean) {
        inCallAudioMode = active
    }

    override fun play(effect: SoundEffect, mode: SoundPlaybackMode) {
        if (mode == SoundPlaybackMode.STOP) {
            activeLoops.remove(effect)?.let { poolFor(effect.category).stop(it) }
            return
        }

        if (effect.category == SoundToneCategory.MESSAGE && inCallAudioMode) return

        val soundId = soundIds[effect] ?: return
        val pool = poolFor(effect.category)
        val streamId = pool.play(
            soundId,
            VOLUME_FULL, VOLUME_FULL,
            PLAYBACK_PRIORITY,
            if (mode == SoundPlaybackMode.LOOP) LOOP_FOREVER else PLAY_ONCE,
            RATE_NORMAL
        )
        if (streamId == 0) {
            Napier.w("Sound cue $effect was not audible (no free stream or not yet loaded)", tag = TAG)
            return
        }
        if (mode == SoundPlaybackMode.LOOP) activeLoops[effect] = streamId
    }

    private fun poolFor(category: SoundToneCategory): SoundPool = when (category) {
        SoundToneCategory.MESSAGE -> uiPool
        SoundToneCategory.CALL -> ringPool
    }

    private fun buildPool(usage: Int, maxStreams: Int): SoundPool = SoundPool.Builder()
        .setMaxStreams(maxStreams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .apply {
            // A clip that will not decode is otherwise invisible: the cue simply never arrives.
            setOnLoadCompleteListener { _, soundId, status ->
                if (status != 0) Napier.w("Sound clip $soundId failed to load (status $status)", tag = TAG)
            }
        }

    /**
     * Which file carries which cue.
     *
     * Exhaustive on purpose: the compiler refuses to build this if an intent is added to [SoundEffect]
     * without a clip, so a cue can never quietly become silence.
     */
    private fun rawIdOf(effect: SoundEffect): Int = when (effect) {
        SoundEffect.MessageIncoming -> R.raw.sound_in
        SoundEffect.MessageOutgoing -> R.raw.sound_out
        SoundEffect.RecordingStarted -> R.raw.voip_recordstart

        // The clip names come from a voice-channel pack. `voicechat_connecting` is the longer cue, so it
        // carries the wait for media to come up, and the short `voip_connecting` carries a brief dropout.
        SoundEffect.CallRingbackLoop -> R.raw.voip_ringback
        SoundEffect.CallConnecting -> R.raw.voicechat_connecting
        SoundEffect.CallReconnecting -> R.raw.voip_connecting
        SoundEffect.CallRemoteAnswered -> R.raw.voicechat_join
        SoundEffect.CallRemoteEnded -> R.raw.voicechat_leave
        SoundEffect.CallEnded -> R.raw.voip_end
        SoundEffect.CallBusy -> R.raw.voip_busy
        SoundEffect.CallFailed -> R.raw.voip_failed
        SoundEffect.CallUnmuteConfirmed -> R.raw.voip_onallowtalk
    }

    private companion object {
        const val TAG = "AndroidSoundPlayer"

        const val MAX_UI_STREAMS = 3
        const val MAX_RING_STREAMS = 2

        const val PLAYBACK_PRIORITY = 1
        const val VOLUME_FULL = 1.0f
        const val RATE_NORMAL = 1.0f
        const val LOOP_FOREVER = -1
        const val PLAY_ONCE = 0
    }
}
