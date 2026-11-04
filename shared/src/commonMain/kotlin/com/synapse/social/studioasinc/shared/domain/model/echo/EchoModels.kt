package com.synapse.social.studioasinc.shared.domain.model.echo

enum class EchoRole {
    HOST,
    SPEAKER,
    AUDIENCE;

    /** Hosts and speakers publish microphone audio; the audience only subscribes. */
    val canPublish: Boolean get() = this != AUDIENCE

    companion object {
        fun fromRaw(raw: String?): EchoRole? = when (raw?.trim()?.lowercase()) {
            "host" -> HOST
            "speaker" -> SPEAKER
            "audience" -> AUDIENCE
            else -> null
        }
    }
}

enum class EchoVisibility {
    PUBLIC,
    UNLISTED;

    companion object {
        fun fromRaw(raw: String?): EchoVisibility? = when (raw?.trim()?.lowercase()) {
            "public" -> PUBLIC
            "unlisted" -> UNLISTED
            else -> null
        }
    }
}

enum class EchoRoomStatus {
    LIVE,
    ENDED;

    companion object {
        fun fromRaw(raw: String?): EchoRoomStatus? = when (raw?.trim()?.lowercase()) {
            "live" -> LIVE
            "ended" -> ENDED
            else -> null
        }
    }
}

/** Why an Echo operation failed, in terms the UI can turn into a string resource. */
enum class EchoErrorKind {
    NOT_AUTHENTICATED,
    FORBIDDEN,
    BLOCKED,
    BANNED,
    NOT_FOUND,
    ENDED,
    STAGE_FULL,
    NOT_AUDIENCE,
    INVALID_INPUT,
    BACKEND_UNREACHABLE,
    AUDIO_FAILED,
    UNKNOWN
}

/**
 * Raised by the Echo data layer for a rejection the backend decided.
 *
 * [kind] is derived from the stable `SYNAPSE_ECHO_*` token the RPCs raise with, so the
 * mapping survives HTTP status codes and localised messages changing underneath it.
 */
class EchoBackendException(
    val kind: EchoErrorKind,
    override val message: String
) : IllegalStateException(message)

data class EchoHostProfile(
    val userId: String,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null
)

data class EchoFollowedMember(
    val userId: String,
    val username: String? = null,
    val avatarUrl: String? = null
)

data class EchoRoom(
    val id: String,
    val hostId: String,
    val title: String,
    val topic: String? = null,
    val visibility: EchoVisibility = EchoVisibility.PUBLIC,
    val status: EchoRoomStatus = EchoRoomStatus.LIVE,
    val maxSpeakers: Int = 8,
    val createdAtMillis: Long = 0L,
    val endedAtMillis: Long? = null
)

data class EchoRoomSummary(
    val room: EchoRoom,
    val host: EchoHostProfile,
    val speakerCount: Int,
    val listenerCount: Int,
    val followedInRoom: List<EchoFollowedMember> = emptyList()
)

data class EchoRoomParticipant(
    val userId: String,
    val role: EchoRole,
    val handRaised: Boolean = false,
    val joinedAtMillis: Long = 0L,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null
) {
    val isOnStage: Boolean get() = role != EchoRole.AUDIENCE

    /** Only the audience can raise a hand, so a stale flag on a speaker is ignored. */
    val hasRaisedHand: Boolean get() = handRaised && role == EchoRole.AUDIENCE
}

data class EchoRoomDetail(
    val room: EchoRoom,
    val host: EchoHostProfile,
    val myRole: EchoRole?,
    /** The caller's app uid, reported by the read: it is not derivable from the auth session. */
    val viewerId: String? = null,
    val participants: List<EchoRoomParticipant> = emptyList()
) {
    val speakers: List<EchoRoomParticipant> get() = participants.filter { it.isOnStage }
    val audience: List<EchoRoomParticipant> get() = participants.filter { !it.isOnStage }
    val speakerCount: Int get() = speakers.size
    val listenerCount: Int get() = audience.size
    val canPublish: Boolean get() = myRole?.canPublish == true
    val isHost: Boolean get() = myRole == EchoRole.HOST

    /** Whether [userId] is the caller; without a viewer id no row is claimed as "you". */
    fun isViewer(userId: String): Boolean = viewerId != null && viewerId == userId
}

/**
 * Connection details for the SFU.
 *
 * The token is a bearer credential for one room and its lifetime; [toString] redacts it so a
 * stray log statement cannot hand someone the ability to join.
 */
data class EchoMediaCredentials(
    val serverUrl: String,
    val token: String
) {
    override fun toString(): String = "EchoMediaCredentials(serverUrl=$serverUrl, token=<redacted>)"
}

data class EchoReaction(
    val userId: String,
    val emoji: String,
    val sentAtMillis: Long = 0L
)
