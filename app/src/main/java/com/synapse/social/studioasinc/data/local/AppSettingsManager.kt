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


    var chatWallpaperType: String
        get() = prefs.getString("chat_wallpaper_type", "DEFAULT") ?: "DEFAULT"
        set(value) = prefs.edit().putString("chat_wallpaper_type", value).apply()

    var chatWallpaperValue: String?
        get() = prefs.getString("chat_wallpaper_value", null)
        set(value) = prefs.edit().putString("chat_wallpaper_value", value).apply()

    var chatWallpaperBlur: Float
        get() = prefs.getFloat("chat_wallpaper_blur", 0f)
        set(value) = prefs.edit().putFloat("chat_wallpaper_blur", value).apply()

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
