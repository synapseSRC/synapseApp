package com.synapse.social.studioasinc.shared.domain.usecase.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendException
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.TurnCredentials
import com.synapse.social.studioasinc.shared.domain.repository.TurnCredentialRepository

/**
 * Gets the ICE configuration for a call, and decides what is good enough to dial with.
 *
 * The rule lives here rather than in the repository because it is a product decision,
 * not a network detail: without a relay server, two peers behind symmetric NATs — the
 * common case on carrier networks and corporate Wi-Fi — fail part-way through
 * negotiation. That presents as a flaky call rather than as missing configuration, so
 * the honest failure comes first. [requireTurn] is the escape hatch for a lab or LAN
 * test where STUN-only genuinely is enough; release call paths pass true.
 *
 * [minValidityMillis] keeps a long call from being handed a credential that expires
 * mid-ring: the repository refreshes at half the server-supplied TTL, and this only
 * asks for as much headroom as the next negotiation needs.
 */
class MintTurnCredentialsUseCase(
    private val repository: TurnCredentialRepository
) {
    suspend operator fun invoke(
        requireTurn: Boolean = true,
        minValidityMillis: Long = DEFAULT_MIN_VALIDITY_MILLIS
    ): Result<TurnCredentials> {
        val obtained = repository.credentials(minValidityMillis)
        val failure = obtained.exceptionOrNull()
        if (failure != null) {
            // A transport failure is the backend being unreachable, not the user being
            // unauthorised; keep them distinguishable in the UI.
            return if (failure is CallBackendException) obtained
            else Result.failure(
                CallBackendException(
                    kind = CallErrorKind.BACKEND_UNREACHABLE,
                    message = "Could not reach the credential service: " +
                        (failure.message ?: failure::class.simpleName.orEmpty())
                )
            )
        }

        val credentials = obtained.getOrThrow()
        if (requireTurn && !credentials.turnAvailable) {
            return Result.failure(
                CallBackendException(
                    kind = CallErrorKind.TURN_UNAVAILABLE,
                    message = "The call service is not configured with a TURN server, so calls " +
                        "behind restrictive networks cannot be connected."
                )
            )
        }
        return Result.success(credentials)
    }

    companion object {
        /** A call is worth dialling only if traversal is guaranteed for the next few minutes. */
        const val DEFAULT_MIN_VALIDITY_MILLIS: Long = 120_000L
    }
}
