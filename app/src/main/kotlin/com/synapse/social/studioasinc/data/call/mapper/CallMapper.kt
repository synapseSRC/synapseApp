package com.synapse.social.studioasinc.data.call.mapper

import com.synapse.social.studioasinc.data.call.dto.CallIceCandidateDto
import com.synapse.social.studioasinc.data.call.dto.CallPeerDto
import com.synapse.social.studioasinc.data.call.dto.CallSessionDto
import com.synapse.social.studioasinc.data.call.dto.CallSignalPayloadDto
import com.synapse.social.studioasinc.data.call.dto.CallSignalingDto
import com.synapse.social.studioasinc.data.call.dto.IceServerDto
import com.synapse.social.studioasinc.data.call.dto.TurnCredentialsDto
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendStatus
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallIceCandidate
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallParticipant
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignal
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalRecord
import com.synapse.social.studioasinc.shared.domain.model.call.IceServerEndpoint
import com.synapse.social.studioasinc.shared.domain.model.call.TurnCredentials
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

/**
 * DTO -> domain conversion for the calling data layer.
 *
 * The two decisions worth stating, because both are places a careless mapper would
 * invent a fact:
 *
 *  1. `phase` is derived from the backend `status`, never from what the client hoped
 *     for. [CallPhase.DIALING] is absent from that table because it only exists while
 *     the row is being created — a local window, so the repository owns it and this
 *     mapper never produces it.
 *  2. An unparseable enum label degrades to the *safe* value, not to a crash and not to
 *     a success: an unknown status reads as ENDED, an unknown end reason as FAILED. A
 *     future server that adds a label must not make an old client show a call as live.
 */
fun CallSessionDto.toDomain(
    currentUserId: String?,
    peerCache: Map<String, CallParticipant> = emptyMap()
): CallSession? {
    val callId = id ?: return null
    val caller = callerId.takeIf { it.isNotBlank() } ?: return null
    val callee = calleeId.takeIf { it.isNotBlank() } ?: return null

    val direction = when (currentUserId) {
        caller -> CallDirection.OUTGOING
        callee -> CallDirection.INCOMING
        // A row we are not party to should never reach here (RLS guarantees it), but
        // guessing a direction for it would be worse than dropping the row.
        else -> return null
    }
    val peerId = if (direction == CallDirection.OUTGOING) callee else caller

    val status = backendStatusOrNull() ?: CallBackendStatus.ENDED
    return CallSession(
        id = callId,
        participant = peerCache[peerId] ?: CallParticipant(userId = peerId, displayName = peerId),
        chatId = chatId.takeIf { it.isNotBlank() },
        direction = direction,
        mediaType = mediaType.toCallMediaType(),
        phase = status.toPhase(direction),
        endReason = endReason.toCallEndReason(),
        startedAtMillis = createdAt.toMillis() ?: System.currentTimeMillis(),
        connectedAtMillis = answeredAt.toMillis(),
        endedAtMillis = endedAt.toMillis(),
        backendStatus = status,
        serverRingTimeoutAtMillis = ringingExpiresAt.toMillis()
    )
}

fun CallPeerDto.toParticipant(): CallParticipant = CallParticipant(
    userId = uid,
    // Falling back to the id rather than a placeholder word keeps the data layer free
    // of copy it has no right to own; the UI is the only place that knows how to name a
    // stranger.
    displayName = displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: username?.trim()?.takeIf { it.isNotEmpty() }
        ?: uid,
    avatarUrl = avatar?.trim()?.takeIf { it.isNotEmpty() }
)

fun CallBackendStatus.toPhase(direction: CallDirection): CallPhase = when (this) {
    // The same word for both sides, and both accurate: outgoing means the peer's device
    // has been told and nobody has picked up, incoming means our own phone is ringing.
    // [direction] is kept in the signature because CONNECTING below is the first place
    // the two sides legitimately diverge.
    CallBackendStatus.RINGING -> CallPhase.RINGING

    CallBackendStatus.ANSWERED -> CallPhase.CONNECTING
    CallBackendStatus.ACTIVE -> CallPhase.ACTIVE
    CallBackendStatus.RECONNECTING -> CallPhase.RECONNECTING
    CallBackendStatus.ENDED -> CallPhase.ENDED
}

fun String?.toBackendStatusOrNull(): CallBackendStatus? = when (this?.lowercase()) {
    "ringing" -> CallBackendStatus.RINGING
    "answered" -> CallBackendStatus.ANSWERED
    "active" -> CallBackendStatus.ACTIVE
    "reconnecting" -> CallBackendStatus.RECONNECTING
    "ended" -> CallBackendStatus.ENDED
    else -> null
}

fun CallSessionDto.backendStatusOrNull(): CallBackendStatus? = status.toBackendStatusOrNull()

fun String?.toCallEndReason(): CallEndReason? = when (this?.lowercase()?.replace('-', '_')) {
    "completed" -> CallEndReason.COMPLETED
    "cancelled" -> CallEndReason.CANCELLED
    "declined" -> CallEndReason.DECLINED
    "no_answer" -> CallEndReason.NO_ANSWER
    "unavailable" -> CallEndReason.UNAVAILABLE
    "failed" -> CallEndReason.FAILED
    "remote_ended" -> CallEndReason.REMOTE_ENDED
    "ended" -> CallEndReason.COMPLETED
    null -> null
    // Unknown label on an ended row: report a failure rather than a completed call,
    // because "it worked" is the one thing a mapper must not assume.
    else -> CallEndReason.FAILED
}

fun CallEndReason.toWireValue(): String = name.lowercase()

fun String?.toCallMediaType(): CallMediaType = when (this?.lowercase()) {
    "video" -> CallMediaType.VIDEO
    else -> CallMediaType.VOICE
}

fun CallMediaType.toWireValue(): String = name.lowercase()

fun String?.toMillis(): Long? = TimestampFormatter.parseInstant(this)?.toEpochMilliseconds()

// ---------------------------------------------------------------------------
// Signaling
// ---------------------------------------------------------------------------

fun CallSignalingDto.toRecord(): CallSignalRecord? {
    val kind = kind.toSignalKind() ?: return null
    if (sessionId.isBlank() || senderUid.isBlank()) return null
    return CallSignalRecord(
        id = id,
        sessionId = sessionId,
        senderUserId = senderUid,
        seq = seq,
        signal = CallSignal(
            kind = kind,
            sdpType = payload.type,
            sdp = payload.sdp,
            candidates = payload.candidates.orEmpty().map { it.toDomain() }
        ),
        createdAtMillis = createdAt.toMillis()
    )
}

fun String?.toSignalKind(): CallSignalKind? = when (this?.lowercase()) {
    "offer" -> CallSignalKind.OFFER
    "answer" -> CallSignalKind.ANSWER
    "ice_batch" -> CallSignalKind.ICE_BATCH
    "ice" -> CallSignalKind.ICE
    else -> null
}

fun CallSignalKind.toWireValue(): String = when (this) {
    CallSignalKind.OFFER -> "offer"
    CallSignalKind.ANSWER -> "answer"
    CallSignalKind.ICE_BATCH -> "ice_batch"
    CallSignalKind.ICE -> "ice"
}

fun CallIceCandidateDto.toDomain(): CallIceCandidate =
    CallIceCandidate(candidate = candidate, sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex)

fun CallIceCandidate.toDto(): CallIceCandidateDto =
    CallIceCandidateDto(candidate = candidate, sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex)

fun CallSignal.toPayloadDto(): CallSignalPayloadDto = CallSignalPayloadDto(
    type = sdpType,
    sdp = sdp,
    candidates = candidates.map { it.toDto() }.takeIf { it.isNotEmpty() }
)

// ---------------------------------------------------------------------------
// TURN credentials
// ---------------------------------------------------------------------------

fun TurnCredentialsDto.toDomain(): TurnCredentials {
    // isTurn is a property of the domain IceServerEndpoint, not of the DTO: derive it from the
    // list that was actually mapped, so a STUN-only payload cannot claim traversal support.
    val servers = iceServers.map { it.toDomain() }
    return TurnCredentials(
        servers = servers,
        expiresAtMillis = expiresAtMillis,
        turnAvailable = turnAvailable || servers.any { it.isTurn }
    )
}

fun IceServerDto.toDomain(): IceServerEndpoint = IceServerEndpoint(
    urls = urls.filter { it.isNotBlank() },
    username = username,
    credential = credential
)

// ---------------------------------------------------------------------------
// Failure classification
// ---------------------------------------------------------------------------
/**
 * Turns a backend rejection into a [CallErrorKind].
 *
 * The RPCs raise with a stable `SYNAPSE_CALL_*` token, which is what this reads: HTTP
 * status codes and Postgres SQLSTATEs both get re-mapped by intermediaries, a message
 * prefix we chose does not. SQLSTATEs are kept as the secondary signal for the cases a
 * token cannot cover (a unique-violation on an envelope retry, RLS rejecting a select).
 */
fun classifyCallFailure(rawMessage: String?, sqlState: String?, httpStatus: Int?): CallErrorKind {
    val message = rawMessage?.uppercase()
    when {
        message != null && "SYNAPSE_CALL_UNAUTHENTICATED" in message -> return CallErrorKind.NOT_AUTHENTICATED
        message != null && "SYNAPSE_CALL_FORBIDDEN" in message -> return CallErrorKind.FORBIDDEN
        message != null && "SYNAPSE_CALL_BLOCKED" in message -> return CallErrorKind.BLOCKED
        message != null && "SYNAPSE_CALL_NOT_FOUND" in message -> return CallErrorKind.NOT_FOUND
        message != null && "SYNAPSE_CALL_ALREADY_ENDED" in message -> return CallErrorKind.ALREADY_ENDED
        message != null && "SYNAPSE_CALL_STATE_INVALID" in message -> return CallErrorKind.STATE_INVALID
        message != null && "SYNAPSE_CALL_BUSY" in message -> return CallErrorKind.BUSY
        message != null && "SYNAPSE_CALL_EXPIRED" in message -> return CallErrorKind.EXPIRED
        message != null && "TURN_NOT_CONFIGURED" in message -> return CallErrorKind.TURN_UNAVAILABLE
    }
    return when {
        httpStatus == 401 || httpStatus == 403 -> CallErrorKind.FORBIDDEN
        sqlState == "42501" -> CallErrorKind.FORBIDDEN
        sqlState == "23505" -> CallErrorKind.BUSY
        sqlState == "P0002" -> CallErrorKind.NOT_FOUND
        else -> CallErrorKind.UNKNOWN
    }
}
