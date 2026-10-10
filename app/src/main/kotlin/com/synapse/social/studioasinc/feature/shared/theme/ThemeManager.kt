package com.synapse.social.studioasinc.feature.shared.theme

import android.app.Activity
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.synapse.social.studioasinc.ui.settings.ThemeMode



object ThemeManager {



    fun applyThemeMode(themeMode: ThemeMode) {
        val nightMode = when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                } else {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
            }
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }



    fun applyDynamicColor(activity: Activity, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled) {



        }
    }
}
