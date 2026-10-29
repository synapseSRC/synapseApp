package com.synapse.social.studioasinc.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.aakira.napier.Napier

/**
 * Helper class for Google Sign-In using Credential Manager API.
 * Handles native Android Google authentication flow.
 */
class GoogleAuthHelper(private val context: Context) {
    
    private val credentialManager = CredentialManager.create(context)
    private val TAG = "GoogleAuthHelper"
    
    /**
     * Initiates Google Sign-In flow using Credential Manager.
     * @param serverClientId The OAuth 2.0 web client ID from Google Cloud Console
     * @return Result containing the ID token on success, or error on failure
     */
    suspend fun signIn(serverClientId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )
            
            val credential = result.credential
            
            // Google Credential Manager returns CustomCredential when the Play Services
            // version predates the typed GoogleIdTokenCredential marshalling.
            val googleCredential = when (credential) {
                is GoogleIdTokenCredential -> credential
                is CustomCredential ->
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } else null
                else -> null
            }
            
            if (googleCredential != null) {
                Napier.d("Google Sign-In successful", tag = TAG)
                Result.success(googleCredential.idToken)
            } else {
                val error = "Unexpected credential type: ${credential::class.simpleName}"
                Napier.e(error, tag = TAG)
                Result.failure(Exception(error))
            }
        } catch (e: GetCredentialException) {
            Napier.e("Google Sign-In failed: ${e.message}", tag = TAG)
            Result.failure(e)
        } catch (e: Exception) {
            Napier.e("Unexpected error during Google Sign-In: ${e.message}", tag = TAG)
            Result.failure(e)
        }
    }
}
