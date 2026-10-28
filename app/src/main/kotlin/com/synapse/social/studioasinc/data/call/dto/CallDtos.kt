package com.synapse.social.studioasinc.data.call.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes for the calling tables and the TURN credential function.
 *
 * These mirror the database exactly, per the repository's DTO rule: field names are
 * the column names, nullability is the column nullability, and nothing here carries
 * behaviour. They never leave the data layer — [com.synapse.social.studioasinc.data.call.mapper.toDomain]
 * converts them.
 *
 * Defaults exist because realtime CDC events and RPC responses are both decoded with
 * `decodeRecord`/`decodeSingle`, and a partial record (a delete carrying only the
 * identity columns, a projection missing an optional field) must not be able to throw
 * a serialization exception inside a live call.
 */

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CallSessionDto(
    val id: String? = null,
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("caller_id") val callerId: String = "",
    @SerialName("callee_id") val calleeId: String = "",
    @SerialName("media_type") val mediaType: String = "voice",
    val status: String = "ringing",
    @SerialName("end_reason") val endReason: String? = null,
    @SerialName("end_initiator") val endInitiator: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("answered_at") val answeredAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("ringing_expires_at") val ringingExpiresAt: String? = null
)

/** One row of `call_signaling`. [senderUid] is stamped by the database, not by us. */
@Serializable
data class CallSignalingDto(
    val id: Long = 0L,
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("sender_uid") val senderUid: String = "",
    val seq: Int = 0,
    val kind: String = "",
    val payload: CallSignalPayloadDto = CallSignalPayloadDto(),
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * The envelope body.
 *
 * Candidate keys stay camelCase on purpose: they are WebRTC's own serialized names,
 * the payload is only ever read by another Synapse client that hands it straight to
 * its engine, and keeping the wire shape identical to the engine's avoids a lossy
 * rename in the middle of negotiation.
 */
@Serializable
data class CallSignalPayloadDto(
    val type: String? = null,
    val sdp: String? = null,
    val candidates: List<CallIceCandidateDto>? = null
)

@Serializable
data class CallIceCandidateDto(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0
)

/** Peer profile columns needed to label a call; `users.uid` is the auth user id. */
@Serializable
data class CallPeerDto(
    val uid: String = "",
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val avatar: String? = null
)

/** Response of the `turn-credentials` Edge Function. */
@Serializable
data class TurnCredentialsDto(
    @SerialName("ice_servers") val iceServers: List<IceServerDto> = emptyList(),
    @SerialName("expires_at") val expiresAtMillis: Long? = null,
    @SerialName("ttl_seconds") val ttlSeconds: Long? = null,
    @SerialName("turn_available") val turnAvailable: Boolean = false
)

@Serializable
data class IceServerDto(
    val urls: List<String> = emptyList(),
    val username: String? = null,
    val credential: String? = null
)

/** Error body the function returns; `error`/`code` match the app's other functions. */
@Serializable
data class TurnCredentialErrorDto(
    val error: String? = null,
    val code: String? = null
)
