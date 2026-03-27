package com.synapse.social.studioasinc.core.util

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.preferences.SettingsPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsPreferences: SettingsPreferences
) {
    fun isChatLocked(chatId: String): Boolean {
        return settingsPreferences.getLockedChatIds().contains(chatId)
    }

    fun lockChat(chatId: String) {
        val currentLocked = settingsPreferences.getLockedChatIds().toMutableSet()
        currentLocked.add(chatId)
        settingsPreferences.setLockedChatIds(currentLocked)
    }

    fun unlockChat(chatId: String) {
        val currentLocked = settingsPreferences.getLockedChatIds().toMutableSet()
        currentLocked.remove(chatId)
        settingsPreferences.setLockedChatIds(currentLocked)
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError(context.getString(R.string.error_authentication))
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.privacy_biometric_lock))
            .setSubtitle(context.getString(R.string.privacy_biometric_lock_subtitle))
            .setNegativeButtonText(context.getString(R.string.settings_cancel))
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
