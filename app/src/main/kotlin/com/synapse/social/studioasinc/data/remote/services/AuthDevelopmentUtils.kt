package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import android.util.Log
import com.synapse.social.studioasinc.BuildConfig

object AuthDevelopmentUtils {
    private const val TAG = "AuthDevUtils"

    fun isDevelopmentBuild(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.APPLICATION_ID.contains(".debug")
    }

    fun logAuthConfig(context: Context) {
        if (!isDevelopmentBuild()) return

        val config = AuthConfig.create(context)
        Log.d(TAG, "=== Authentication Configuration ===")
        Log.d(TAG, "Development Mode: ${config.developmentMode}")
        Log.d(TAG, "Email Verification Required: ${config.requireEmailVerification}")
        Log.d(TAG, "Email Verification Bypass: ${config.shouldBypassEmailVerification()}")
        Log.d(TAG, "Debug Logging: ${config.enableDebugLogging}")
        Log.d(TAG, "Resend Cooldown: ${config.resendCooldownSeconds}s")
        Log.d(TAG, "Max Resend Attempts: ${config.maxResendAttempts}")
        Log.d(TAG, "Auto Retry Attempts: ${config.autoRetryAttempts}")
        Log.d(TAG, "Retry Delay: ${config.retryDelayMs}ms")
        Log.d(TAG, "Build Variant: ${if (BuildConfig.DEBUG) "debug" else "release"}")
        Log.d(TAG, "Application ID: ${BuildConfig.APPLICATION_ID}")
        Log.d(TAG, "===================================")
    }
}
