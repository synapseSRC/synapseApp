package com.synapse.social.studioasinc.core.ui.base

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.feature.shared.theme.AuthTheme
import com.synapse.social.studioasinc.feature.shared.theme.ThemeManager
import kotlinx.coroutines.launch

abstract class BaseComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyThemeFromSettings()
    }

    protected fun setSynapseContent(content: @Composable () -> Unit) {
        setContent {
            val settingsRepository = SettingsRepositoryImpl.getInstance(this)
            val appearanceSettings by settingsRepository.appearanceSettings.collectAsState(
                initial = com.synapse.social.studioasinc.ui.settings.AppearanceSettings()
            )

            val darkTheme = when (appearanceSettings.themeMode) {
                com.synapse.social.studioasinc.ui.settings.ThemeMode.LIGHT -> false
                com.synapse.social.studioasinc.ui.settings.ThemeMode.DARK -> true
                com.synapse.social.studioasinc.ui.settings.ThemeMode.SYSTEM ->
                    isSystemInDarkTheme()
            }

            val dynamicColor = appearanceSettings.dynamicColorEnabled &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            SynapseTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                content()
            }
        }
    }

    protected fun setAuthContent(content: @Composable () -> Unit) {
        setContent {
            AuthTheme(enableEdgeToEdge = true) {
                content()
            }
        }
    }

    private fun applyThemeFromSettings() {
        val settingsRepository = SettingsRepositoryImpl.getInstance(this)
        lifecycleScope.launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    ThemeManager.applyThemeMode(settings.themeMode)
                }
            } catch (e: Exception) {
                android.util.Log.e("BaseComposeActivity", "Failed to apply theme from settings", e)
            }
        }
    }
}
