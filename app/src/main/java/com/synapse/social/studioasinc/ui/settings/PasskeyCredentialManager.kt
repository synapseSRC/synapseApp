package com.synapse.social.studioasinc.ui.settings

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasskeyCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun createPasskey(activityContext: Context, requestJson: String): CreatePublicKeyCredentialResponse? {
        Log.d("PasskeyCredentialManager", "Requesting credential creation with JSON: $requestJson")
        val request = CreatePublicKeyCredentialRequest(requestJson)

        return try {
            val response = credentialManager.createCredential(activityContext, request)
            if (response is CreatePublicKeyCredentialResponse) {
                Log.d("PasskeyCredentialManager", "Credential created successfully")
                response
            } else {
                Log.e("PasskeyCredentialManager", "Unexpected response type from Credential Manager")
                null
            }
        } catch (e: CreateCredentialException) {
            Log.e("PasskeyCredentialManager", "Credential Manager Error", e)
            throw e
        }
    }
}
