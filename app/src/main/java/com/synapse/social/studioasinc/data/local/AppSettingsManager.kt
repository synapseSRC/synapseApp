package com.synapse.social.studioasinc.data.local

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Singleton

@Singleton
class AppSettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )

    companion object {
        @Volatile
        private var instance: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager {
            return instance ?: synchronized(this) {
                instance ?: AppSettingsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
