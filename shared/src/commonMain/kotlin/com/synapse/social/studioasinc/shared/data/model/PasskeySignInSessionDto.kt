package com.synapse.social.studioasinc.shared.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors the session object returned by the backend after a successful passkey authentication.
 * Only the token pair the app needs is modeled; the rest of the session payload is ignored.
 */
@Serializable
data class PasskeySignInSessionDto(
    val access_token: String,
    val refresh_token: String
)
