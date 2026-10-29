package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.call.TurnCredentials

/**
 * Obtains the ICE server list a call should use, including traversal credentials.
 *
 * The contract is deliberately narrow and has no "give me a STUN server" fallback:
 * an implementation that returns `turnAvailable = false` is reporting a
 * misconfiguration, and the caller is expected to fail the call rather than
 * negotiate without relay.
 */
interface TurnCredentialRepository {

    /**
     * Mint or reuse credentials valid until at least [minValidityMillis] from now, so a
     * call that is about to start is never handed something already expiring.
     */
    suspend fun credentials(minValidityMillis: Long = 0): Result<TurnCredentials>

    /** Drop any cached material; called when a call ends or a credential is rejected. */
    fun invalidate()
}
