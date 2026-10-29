package com.synapse.social.studioasinc.shared.domain.service

import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPlaybackMode

/**
 * Makes a [SoundEffect] audible.
 *
 * Implementations own everything platform-shaped about that: which file carries which cue, how many
 * cues can overlap, and which audio route each one belongs to. Whether a cue *should* play is decided
 * above this, by `PlaySoundEffectUseCase`.
 */
interface SoundPlayer {
    /**
     * Plays [effect] as [mode] dictates.
     *
     * A [SoundPlaybackMode.STOP] for an effect that is not currently looping does nothing, so a missed
     * transition cannot leave a tone running and no caller has to know what was already playing.
     */
    fun play(effect: SoundEffect, mode: SoundPlaybackMode)
}
