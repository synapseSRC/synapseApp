package com.synapse.social.studioasinc.shared.domain.repository

/**
 * Platform abstraction for WebAuthn passkey operations (Credential Manager on Android,
 * ASAuthorizationController on iOS). Kept as a plain interface so Domain stays free of
 * any platform SDK. Platform implementations are injected via DI.
 */
interface PasskeyPlatformManager {

    /** Whether the current platform/device can create and use passkeys. */
    val isSupported: Boolean

    /**
     * Creates a new resident passkey credential from the given WebAuthn
     * PublicKeyCredentialCreationOptions JSON. Returns the registration
     * response JSON, or throws if the user cancels or the platform fails.
     */
    suspend fun createCredential(requestJson: String): String

    /**
     * Performs a passkey assertion from the given WebAuthn
     * PublicKeyCredentialRequestOptions JSON. Returns the authentication
     * response JSON, or throws if the user cancels or no credential matches.
     */
    suspend fun assertCredential(optionsJson: String): String
}
