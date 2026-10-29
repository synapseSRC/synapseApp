package com.synapse.social.studioasinc.shared.domain.usecase.sound

import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPlaybackMode
import com.synapse.social.studioasinc.shared.domain.repository.SoundSettingsRepository
import com.synapse.social.studioasinc.shared.domain.service.SoundPlayer
import com.synapse.social.studioasinc.shared.util.TimeProvider

/**
 * Decides whether a cue is allowed to be heard right now, then hands it to the [SoundPlayer].
 *
 * Every reason a sound does *not* play lives here, so a trigger site can stay a plain statement of what
 * happened. Two of those reasons are worth naming:
 *
 * - a switch the user turned off;
 * - a burst of arrivals that would otherwise produce a burst of identical blips. The realtime stream
 *   emits both inserts and updates, and an open chat and the inbox list can each see the same message,
 *   so a per-cue cooldown is what turns one arrival into one sound.
 *
 * [SoundPlaybackMode.STOP] is never refused. A cue started while sound was on has to stay stoppable
 * after the user turns sound off, or the app is left ringing a tone nobody can cancel.
 */
class PlaySoundEffectUseCase(
    private val player: SoundPlayer,
    private val settings: SoundSettingsRepository
) {
    private val lastPlayedAt = mutableMapOf<SoundEffect, Long>()

    suspend operator fun invoke(
        effect: SoundEffect,
        mode: SoundPlaybackMode = SoundPlaybackMode.ONCE
    ) {
        if (mode == SoundPlaybackMode.STOP) {
            stop(effect)
            return
        }

        if (!settings.currentPreferences().isCategoryEnabled(effect.category)) return

        val cooldown = effect.cooldownMillis
        if (cooldown > 0L) {
            val now = TimeProvider.nowMillis()
            val previous = lastPlayedAt[effect]
            if (previous != null && now - previous < cooldown) return
            lastPlayedAt[effect] = now
        }

        player.play(effect, mode)
    }

    /**
     * Silences a looping cue without reading a single setting first.
     *
     * [invoke] is suspend because it asks the preference store what the user allows. Silence needs no
     * permission and cannot wait on one: a ViewModel losing its scope as it is cleared would otherwise be
     * unable to stop its own ringback, leaving a tone nobody can reach.
     */
    fun stop(effect: SoundEffect) {
        player.play(effect, SoundPlaybackMode.STOP)
    }

    /**
     * How long a repeat of the same cue is ignored.
     *
     * Only cues that can arrive several times in a breath carry one. A send, or a call transition, is
     * already one sound per deliberate event, and putting those behind a cooldown would swallow a real
     * transition that followed quickly — answering a call seconds after dialling it, for example.
     */
    private val SoundEffect.cooldownMillis: Long
        get() = when (this) {
            SoundEffect.MessageIncoming -> ARRIVAL_COOLDOWN_MS
            SoundEffect.RecordingStarted -> RECORDING_COOLDOWN_MS
            else -> 0L
        }

    private companion object {
        const val ARRIVAL_COOLDOWN_MS = 1_200L
        const val RECORDING_COOLDOWN_MS = 300L
    }
}
