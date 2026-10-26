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
}
