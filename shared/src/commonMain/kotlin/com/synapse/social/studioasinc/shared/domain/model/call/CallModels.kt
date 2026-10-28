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

/** Reasons where nobody got through, so an incoming one reads to the receiver as a missed call. */
val CallEndReason.isUnanswered: Boolean
    get() = this == CallEndReason.NO_ANSWER || this == CallEndReason.DECLINED || this == CallEndReason.CANCELLED ||
        this == CallEndReason.UNAVAILABLE || this == CallEndReason.FAILED

/**
 * Reads back the status the backend wrote onto a call log message.
 *
 * Returns null for anything unrecognised so a caller can fall back to a generic label, rather than
 * printing a raw enum name to the user.
 */
fun callEndReasonFromRaw(raw: String?): CallEndReason? {
    val key = raw?.trim()?.uppercase()?.replace('-', '_')?.replace(' ', '_') ?: return null
    if (key == "MISSED") return CallEndReason.NO_ANSWER
    return CallEndReason.entries.firstOrNull { it.name == key }
}

/** Only the receiver of a ring nobody answered has a missed call; the caller simply got no answer. */
fun isMissedCall(reason: CallEndReason?, connected: Boolean, isIncoming: Boolean): Boolean =
    !connected && isIncoming && reason?.isUnanswered == true

enum class CallAudioRoute { EARPIECE, SPEAKER, BLUETOOTH, WIRED_HEADSET }

enum class CallConnectionQuality { GOOD, FAIR, POOR }

/**
 * The call state as the backend recorded it.
 *
 * Deliberately separate from [CallPhase]: the phase is what this device renders,
 * this is what the server asserts. A UI may only reach [CallPhase.ACTIVE] once the
 * backend says [ACTIVE], which is what keeps a local timer, a retry loop or a
 * dropped response from ever producing a call that did not happen.
 */
enum class CallBackendStatus { RINGING, ANSWERED, ACTIVE, RECONNECTING, ENDED }

/**
 * Why a call operation failed, in terms the UI can turn into a string resource.
 *
 * The alternatives — surfacing a raw exception message or collapsing everything to
 * one failure — both lose the distinction the user actually needs between "this
 * person is busy", "your connection dropped" and "we could not reach the call
 * service".
 */
enum class CallErrorKind {
    NOT_AUTHENTICATED,
    FORBIDDEN,
    BLOCKED,
    NOT_FOUND,
    ALREADY_ENDED,
    STATE_INVALID,
    BUSY,
    EXPIRED,
    BACKEND_UNREACHABLE,
    TURN_UNAVAILABLE,
    SIGNALLING_FAILED,
    ICE_FAILED,
    MEDIA_START_FAILED,
    PERMISSION_DENIED,
    UNKNOWN
}

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

/**
 * Raised by the calling data layer for a rejection that the backend decided.
 *
 * [kind] is derived from the stable `SYNAPSE_CALL_*` token the RPCs raise with, so
 * the mapping survives HTTP status codes and localised messages changing underneath
 * it. It stays in the domain because deciding *what a failure means* is business
 * logic; translating it into a Postgres error code is not.
 */
class CallBackendException(
    val kind: CallErrorKind,
    override val message: String
) : IllegalStateException(message)

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
    val endedAtMillis: Long? = null,
    /**
     * Backend-confirmed status for this session, or null while nothing is
     * persisted yet — which is exactly the caller's local DIALING window.
     */
    val backendStatus: CallBackendStatus? = null,
    /** Last failure affecting this session, for the UI's error surface. */
    val errorKind: CallErrorKind? = null,
    /**
     * `ringing_expires_at` from the backend. No-answer timing reads this instead of
     * a client-side constant, so a device with a wrong clock cannot end a call early
     * or keep ringing forever.
     */
    val serverRingTimeoutAtMillis: Long? = null
)
