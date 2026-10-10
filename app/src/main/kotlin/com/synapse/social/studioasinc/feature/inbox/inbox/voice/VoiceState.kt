package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import java.io.File

sealed interface VoiceState {
    object Normal : VoiceState
    object VoiceReady : VoiceState
    object Pressing : VoiceState

    data class Recording(
        val durationMs: Long = 0L,
        val amplitudeHistory: List<Float> = emptyList(),
        val slideOffsetPx: Float = 0f,
        val dragOffsetYPx: Float = 0f,
        val isArmedForCancel: Boolean = false,
        val cancelProgress: Float = 0f,
        val lockProgress: Float = 0f
    ) : VoiceState

    data class Locked(
        val durationMs: Long = 0L,
        val amplitudeHistory: List<Float> = emptyList(),
        val isPaused: Boolean = false
    ) : VoiceState

    data class VoiceDraft(
        val audioFile: File,
        val durationMs: Long,
        val amplitudeHistory: List<Float>,
        val playbackPositionMs: Long = 0L,
        val isPlaying: Boolean = false
    ) : VoiceState

    data class Sending(val audioFile: File) : VoiceState

    data class Failed(
        val error: String,
        val draftFile: File? = null,
        val durationMs: Long = 0L
    ) : VoiceState
}
