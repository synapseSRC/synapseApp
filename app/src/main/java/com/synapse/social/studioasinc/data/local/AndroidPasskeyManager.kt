package com.synapse.social.studioasinc.data.local

import android.content.Context
import androidx.credentials.exceptions.CreateCredentialException
import com.synapse.social.studioasinc.domain.repository.PasskeyManager
import com.synapse.social.studioasinc.ui.settings.PasskeyCredentialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPasskeyManager @Inject constructor(
    private val passkeyCredentialManager: PasskeyCredentialManager,
    @ApplicationContext private val context: Context
) : PasskeyManager {
    override suspend fun createPasskey(requestJson: String): String? {
        return try {
            passkeyCredentialManager.createPasskey(context, requestJson)?.registrationResponseJson
        } catch (e: CreateCredentialException) {
            throw Exception("Passkey creation failed: ${e.message}")
        }
    }
}
