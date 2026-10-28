package com.synapse.social.studioasinc.shared.domain.model.call

/**
 * One entry of the ICE server list a media session should use.
 *
 * The shape mirrors what an ICE configuration consumes (a set of URLs plus an
 * optional credential pair) without naming any WebRTC type, so the domain stays
 * compilable on every platform.
 */
data class IceServerEndpoint(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
) {
    val isTurn: Boolean get() = urls.any { it.startsWith("turn:", true) || it.startsWith("turns:", true) }

    /** Never log this: [credential] is secret material while it is still valid. */
    override fun toString(): String = "IceServerEndpoint(urls=$urls, secured=$isTurn)"
}

/**
 * Short-lived traversal credentials minted per call by the backend.
 *
 * [turnAvailable] is the honest answer to "will this call survive a restrictive
 * NAT?". A client must not treat a missing TURN server as a detail to paper over:
 * two peers behind symmetric NATs will then fail half-way through negotiation
 * rather than up front, which reads as a flaky product instead of a missing
 * configuration. Callers are expected to fail visibly when it is false.
 *
 * [expiresAtMillis] is server-supplied. Credentials are refreshed at half their
 * lifetime so a long call never runs out mid-conversation, and rotation is applied
 * with an ICE restart instead of a new session.
 */
data class TurnCredentials(
    val servers: List<IceServerEndpoint>,
    val expiresAtMillis: Long? = null,
    val turnAvailable: Boolean
) {
    companion object {
        fun unavailable(): TurnCredentials = TurnCredentials(servers = emptyList(), turnAvailable = false)
    }
}
