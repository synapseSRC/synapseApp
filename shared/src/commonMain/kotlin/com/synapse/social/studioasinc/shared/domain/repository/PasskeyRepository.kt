package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.model.auth.PasskeyCeremonyOptions
import com.synapse.social.studioasinc.shared.domain.model.auth.PasskeySignInSession

/**
 * Backend-neutral passkey service. The ceremony is split into "options" (fetch a WebAuthn
 * challenge from the backend) and "verify" (send the signed credential back), matching the
 * native passkey flow. Which user acts is derived from the current session, so no user id is
 * threaded through here.
 */
interface PasskeyRepository {

    /**
     * WebAuthn creation options for enrolling a new passkey for the signed-in user.
     * [friendlyName] labels the credential shown in [listPasskeys].
     */
    suspend fun registrationOptions(friendlyName: String): Result<PasskeyCeremonyOptions>

    /** Validates a registration credential response and stores it against the current user. */
    suspend fun verifyRegistration(challengeId: String, credentialJson: String): Result<Unit>

    /** WebAuthn request options for a discoverable-credential sign-in. */
    suspend fun authenticationOptions(): Result<PasskeyCeremonyOptions>

    /** Validates an authentication credential response and returns a new session. */
    suspend fun verifyAuthentication(challengeId: String, credentialJson: String): Result<PasskeySignInSession>

    /** The passkeys currently registered for the signed-in user. */
    suspend fun listPasskeys(): Result<List<Passkey>>

    /** Revokes a single passkey by id. */
    suspend fun deletePasskey(id: String): Result<Unit>
}
