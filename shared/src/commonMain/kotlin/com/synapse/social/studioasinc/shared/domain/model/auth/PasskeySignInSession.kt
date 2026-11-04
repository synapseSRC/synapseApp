package com.synapse.social.studioasinc.shared.domain.model.auth

/**
 * Session tokens returned by the backend after a successful passkey authentication.
 * Provider-neutral: no backend-specific semantics leak into Domain.
 */
data class PasskeySignInSession(
    val accessToken: String,
    val refreshToken: String
)
