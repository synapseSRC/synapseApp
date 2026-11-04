package com.synapse.social.studioasinc.shared.data.passkey

import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager

/**
 * Fallback for platforms without a WebAuthn passkey provider (JVM desktop, WASM).
 * All operations fail fast so callers can hide passkey entry points via [isSupported].
 */
class UnsupportedPasskeyPlatformManager : PasskeyPlatformManager {

    override val isSupported: Boolean = false

    override suspend fun createCredential(requestJson: String): String {
        throw UnsupportedOperationException("Passkeys are not supported on this platform")
    }

    override suspend fun assertCredential(optionsJson: String): String {
        throw UnsupportedOperationException("Passkeys are not supported on this platform")
    }
}
