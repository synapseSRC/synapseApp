package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synapse.social.studioasinc.ui.settings.AppearanceSettings
import com.synapse.social.studioasinc.ui.settings.FontScale
import com.synapse.social.studioasinc.ui.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AppearanceStore {
    val themeMode: Flow<ThemeMode>
    val dynamicColorEnabled: Flow<Boolean>
    val fontScale: Flow<FontScale>
    val appearanceSettings: Flow<AppearanceSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setFontScale(scale: FontScale)
    suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.ui.settings.PostViewStyle)
}

class AppearanceStoreImpl(private val dataStore: DataStore<Preferences>) : AppearanceStore {
    override val themeMode: Flow<ThemeMode> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_THEME_MODE]?.let { value ->
            runCatching { ThemeMode.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_THEME_MODE)
        } ?: SettingsConstants.DEFAULT_THEME_MODE
    }

    override val dynamicColorEnabled: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_DYNAMIC_COLOR_ENABLED] ?: SettingsConstants.DEFAULT_DYNAMIC_COLOR_ENABLED
    }

    override val fontScale: Flow<FontScale> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_FONT_SCALE]?.let { value ->
            runCatching { FontScale.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_FONT_SCALE)
        } ?: SettingsConstants.DEFAULT_FONT_SCALE
    }

    override val appearanceSettings: Flow<AppearanceSettings> = dataStore.safePreferencesFlow().map { preferences ->
        AppearanceSettings(
            themeMode = preferences[SettingsConstants.KEY_THEME_MODE]?.let { value ->
                runCatching { ThemeMode.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_THEME_MODE)
            } ?: SettingsConstants.DEFAULT_THEME_MODE,
            dynamicColorEnabled = preferences[SettingsConstants.KEY_DYNAMIC_COLOR_ENABLED] ?: SettingsConstants.DEFAULT_DYNAMIC_COLOR_ENABLED,
            fontScale = preferences[SettingsConstants.KEY_FONT_SCALE]?.let { value ->
                runCatching { FontScale.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_FONT_SCALE)
            } ?: SettingsConstants.DEFAULT_FONT_SCALE,
            postViewStyle = preferences[SettingsConstants.KEY_POST_VIEW_STYLE]?.let { value ->
                runCatching { com.synapse.social.studioasinc.ui.settings.PostViewStyle.valueOf(value) }
                    .getOrDefault(com.synapse.social.studioasinc.ui.settings.PostViewStyle.SWIPE)
            } ?: com.synapse.social.studioasinc.ui.settings.PostViewStyle.SWIPE
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_THEME_MODE] = mode.name
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    override suspend fun setFontScale(scale: FontScale) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_FONT_SCALE] = scale.name
        }
    }

    override suspend fun setPostViewStyle(style: com.synapse.social.studioasinc.ui.settings.PostViewStyle) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_POST_VIEW_STYLE] = style.name
        }
    }
}
