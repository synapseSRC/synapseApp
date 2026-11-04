package com.synapse.social.studioasinc.shared.data.passkey

import android.app.Activity
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyPlatformManager

/**
 * Android implementation of [PasskeyPlatformManager] backed by the Jetpack
 * Credential Manager API. Plain class without DI annotations — it is wired
 * through Koin in [com.synapse.social.studioasinc.shared.di.PasskeyPlatformModule.android].
 */
class AndroidPasskeyPlatformManager(
    private val activityProvider: PasskeyActivityProvider
) : PasskeyPlatformManager {

    override val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    override suspend fun createCredential(requestJson: String): String {
        val activity = requireActivity()
        val credentialManager = CredentialManager.create(activity)
        val request = CreatePublicKeyCredentialRequest(requestJson)

        val response = try {
            credentialManager.createCredential(activity, request)
        } catch (e: CreateCredentialCancellationException) {
            throw IllegalStateException("Passkey creation was cancelled by the user", e)
        } catch (e: CreateCredentialException) {
            throw IllegalStateException("Failed to create passkey: ${e.message}", e)
        }

        return when (response) {
            is CreatePublicKeyCredentialResponse -> response.registrationResponseJson
            else -> throw IllegalStateException(
                "Unexpected credential response type: ${response.type}"
            )
        }
    }

    override suspend fun assertCredential(optionsJson: String): String {
        val activity = requireActivity()
        val credentialManager = CredentialManager.create(activity)
        val request = GetCredentialRequest(
            listOf(
                GetPublicKeyCredentialOption(optionsJson, null, emptySet())
            )
        )

        val response = try {
            credentialManager.getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            throw IllegalStateException("Passkey sign-in was cancelled by the user", e)
        } catch (e: NoCredentialException) {
            throw IllegalStateException("No passkey was found for this account on this device", e)
        } catch (e: GetCredentialException) {
            throw IllegalStateException("Failed to sign in with passkey: ${e.message}", e)
        }

        val publicKeyCredential = response.credential as? PublicKeyCredential
            ?: throw IllegalStateException(
                "Unexpected credential response type: ${response.credential.type}"
            )
        return publicKeyCredential.authenticationResponseJson
    }

    /**
     * Credential Manager can only present the biometric prompt from a live screen, so a ceremony
     * started with none in the foreground is refused rather than silently dropped.
     */
    private fun requireActivity(): Activity =
        activityProvider.currentActivity
            ?: throw IllegalStateException("No screen is open to show the passkey prompt")
}
