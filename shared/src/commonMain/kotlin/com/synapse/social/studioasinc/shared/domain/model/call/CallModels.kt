package com.synapse.social.studioasinc.shared.domain.model.call

enum class CallDirection { INCOMING, OUTGOING }

enum class CallMediaType { VOICE, VIDEO }

enum class CallPhase { DIALING, RINGING, CONNECTING, ACTIVE, RECONNECTING, ENDED }

enum class CallEndReason {
    COMPLETED,
    CANCELLED,
    DECLINED,
    NO_ANSWER,
    UNAVAILABLE,
    FAILED,
    REMOTE_ENDED
}

enum class CallAudioRoute { EARPIECE, SPEAKER, BLUETOOTH, WIRED_HEADSET }

enum class CallConnectionQuality { GOOD, FAIR, POOR }

data class CallParticipant(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null
)

data class CallRequest(
    val participant: CallParticipant,
    val chatId: String? = null,
    val direction: CallDirection,
    val mediaType: CallMediaType
)

data class CallSession(
    val id: String,
    val participant: CallParticipant,
    val chatId: String? = null,
    val direction: CallDirection,
    val mediaType: CallMediaType,
    val phase: CallPhase,
    val endReason: CallEndReason? = null,
    val isMuted: Boolean = false,
    val audioRoute: CallAudioRoute = CallAudioRoute.EARPIECE,
    val isLocalVideoEnabled: Boolean = false,
    val isRemoteVideoEnabled: Boolean = false,
    val isFrontCamera: Boolean = true,
    val connectionQuality: CallConnectionQuality = CallConnectionQuality.GOOD,
    val startedAtMillis: Long,
    val connectedAtMillis: Long? = null,
    val endedAtMillis: Long? = null
)
