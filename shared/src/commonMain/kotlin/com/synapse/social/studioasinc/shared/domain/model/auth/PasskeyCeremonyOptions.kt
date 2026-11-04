package com.synapse.social.studioasinc.shared.domain.model.auth

/**
 * A WebAuthn ceremony challenge handed back by the backend, ready to be fed to the
 * platform [com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager].
 *
 * [challengeId] correlates the option request with the later verify call; [optionsJson] is the
 * raw WebAuthn PublicKeyCredentialCreationOptions / RequestOptions (base64url encoded fields).
 * Provider-neutral: the JSON is a standard WebAuthn shape, not a backend-specific type.
 */
data class PasskeyCeremonyOptions(
    val challengeId: String,
    val optionsJson: String
)
