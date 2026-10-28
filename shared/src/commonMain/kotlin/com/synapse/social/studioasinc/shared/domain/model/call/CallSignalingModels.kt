package com.synapse.social.studioasinc.shared.domain.model.call

/**
 * A signaling message exchanged between two peers to establish media.
 *
 * Deliberately expressed as plain strings: SDP is text and ICE candidates are text,
 * so the domain can carry a negotiation without learning anything about WebRTC,
 * Supabase, or any other transport. The media engine converts at its own boundary.
 */
enum class CallSignalKind { OFFER, ANSWER, ICE_BATCH, ICE }

/** One ICE candidate as the engine produced it, transport-neutral. */
data class CallIceCandidate(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0
)

data class CallSignal(
    val kind: CallSignalKind,
    val sdpType: String? = null,
    val sdp: String? = null,
    val candidates: List<CallIceCandidate> = emptyList()
) {
    init {
        when (kind) {
            CallSignalKind.OFFER, CallSignalKind.ANSWER ->
                require(!sdp.isNullOrBlank()) { "A $kind envelope needs an SDP body" }

            CallSignalKind.ICE, CallSignalKind.ICE_BATCH ->
                require(candidates.isNotEmpty()) { "A $kind envelope needs at least one candidate" }
        }
    }

    companion object {
        fun description(kind: CallSignalKind, sdpType: String?, sdp: String) =
            CallSignal(kind = kind, sdpType = sdpType, sdp = sdp)

        fun batch(candidates: List<CallIceCandidate>) =
            CallSignal(kind = CallSignalKind.ICE_BATCH, candidates = candidates)

        fun single(candidate: CallIceCandidate) =
            CallSignal(kind = CallSignalKind.ICE, candidates = listOf(candidate))
    }
}

/**
 * A signaling envelope as stored by the backend.
 *
 * [senderUserId] is stamped by the server, not by the peer, which is why a receiver
 * can trust it — and why the receiver still has to filter it: the transport echoes
 * your own writes back to you, so ignoring [isFromSelf] would mean answering your
 * own offer.
 *
 * [id] is the backend's monotonic row id and doubles as the replay cursor: after a
 * websocket drop the client re-reads everything with a greater id before resuming
 * live delivery, so a candidate that arrived while offline is not silently lost.
 */
data class CallSignalRecord(
    val id: Long,
    val sessionId: String,
    val senderUserId: String,
    val seq: Int,
    val signal: CallSignal,
    val createdAtMillis: Long? = null
) {
    fun isFromSelf(currentUserId: String?): Boolean =
        !currentUserId.isNullOrBlank() && senderUserId == currentUserId
}
