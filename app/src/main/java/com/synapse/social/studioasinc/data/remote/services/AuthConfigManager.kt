package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import android.util.Log



class AuthConfigManager(private val context: Context) {

    companion object {
        private const val TAG = "AuthConfigManager"

        @Volatile
        private var INSTANCE: AuthConfigManager? = null

        fun getInstance(context: Context): AuthConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var currentConfig: AuthConfig = AuthConfig.create(context)



    fun getConfig(): AuthConfig = currentConfig



    fun updateConfig(newConfig: AuthConfig) {
        currentConfig = newConfig
        AuthConfig.save(context, newConfig)

        if (newConfig.isDebugLoggingEnabled()) {
            Log.d(TAG, "Configuration updated: $newConfig")
        }
    }



    fun reloadConfig() {
        currentConfig = AuthConfig.create(context)

        if (currentConfig.isDebugLoggingEnabled()) {
            Log.d(TAG, "Configuration reloaded: $currentConfig")
        }
    }



    fun enableDevelopmentMode(): AuthConfig {
        currentConfig = AuthConfig.enableDevelopmentMode(context)
        Log.i(TAG, "Development mode enabled - email verification bypassed")
        return currentConfig
    }



    fun disableDevelopmentMode(): AuthConfig {
        currentConfig = AuthConfig.disableDevelopmentMode(context)
        Log.i(TAG, "Development mode disabled - production settings restored")
        return currentConfig
    }



    fun toggleDevelopmentMode(): AuthConfig {
        return if (currentConfig.developmentMode) {
            disableDevelopmentMode()
        } else {
            enableDevelopmentMode()
        }
    }



    fun resetToDefaults(): AuthConfig {
        currentConfig = AuthConfig.reset(context)
        Log.i(TAG, "Configuration reset to defaults")
        return currentConfig
    }



    fun getConfigSummary(): String {
        return buildString {
            appendLine("Authentication Configuration Summary:")
            appendLine("- Development Mode: ${currentConfig.developmentMode}")
            appendLine("- Email Verification Required: ${currentConfig.requireEmailVerification}")
            appendLine("- Debug Logging Enabled: ${currentConfig.enableDebugLogging}")
            appendLine("- Resend Cooldown: ${currentConfig.resendCooldownSeconds}s")
            appendLine("- Max Resend Attempts: ${currentConfig.maxResendAttempts}")
            appendLine("- Auto Retry Attempts: ${currentConfig.autoRetryAttempts}")
            appendLine("- Retry Delay: ${currentConfig.retryDelayMs}ms")
            appendLine("- Email Verification Bypass: ${currentConfig.shouldBypassEmailVerification()}")
        }
    }



    fun logCurrentConfig() {
        if (currentConfig.isDebugLoggingEnabled()) {
            Log.d(TAG, getConfigSummary())
        }
    }



    fun createDevelopmentConfig(): AuthConfig {
        return currentConfig.copy(
            developmentMode = true,
            requireEmailVerification = false,
            enableDebugLogging = true,
            resendCooldownSeconds = 30,
            maxResendAttempts = 10,
            autoRetryAttempts = 5,
            retryDelayMs = 500L
        )
    }



    fun createProductionConfig(): AuthConfig {
        return currentConfig.copy(
            developmentMode = false,
            requireEmailVerification = true,
            enableDebugLogging = false,
            resendCooldownSeconds = 60,
            maxResendAttempts = 5,
            autoRetryAttempts = 3,
            retryDelayMs = 1000L
        )
    }



    fun applyDevelopmentConfig() {
        updateConfig(createDevelopmentConfig())
        Log.i(TAG, "Development configuration applied")
    }



    fun applyProductionConfig() {
        updateConfig(createProductionConfig())
        Log.i(TAG, "Production configuration applied")
    }



    fun isDevelopmentFriendly(): Boolean {
        return currentConfig.developmentMode &&
               currentConfig.shouldBypassEmailVerification() &&
               currentConfig.isDebugLoggingEnabled()
    }



    fun isProductionReady(): Boolean {
        return !currentConfig.developmentMode &&
               currentConfig.requireEmailVerification &&
               !currentConfig.enableDebugLogging
    }
}
