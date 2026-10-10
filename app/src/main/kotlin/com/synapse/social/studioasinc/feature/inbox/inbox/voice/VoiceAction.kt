package com.synapse.social.studioasinc.feature.inbox.inbox.voice

sealed interface VoiceAction {
    object OpenVoiceMode : VoiceAction
    object CloseVoiceMode : VoiceAction
    object StartRecording : VoiceAction
    data class Dragged(val deltaX: Float, val deltaY: Float) : VoiceAction
    object ReleaseRecording : VoiceAction
    object CancelRecording : VoiceAction
    object TogglePause : VoiceAction
    object FinishRecording : VoiceAction
    object DiscardDraft : VoiceAction
    object TogglePlayDraft : VoiceAction
    data class SeekDraft(val positionMs: Long) : VoiceAction
    object SendDraft : VoiceAction
    data class ErrorOccurred(val message: String) : VoiceAction
    object RequestPermission : VoiceAction
    data class PermissionResult(val isGranted: Boolean) : VoiceAction
}
