package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ChatStore {
    val chatFontScale: Flow<Float>
    val chatThemePreset: Flow<ChatThemePreset>
    val chatWallpaperType: Flow<WallpaperType>
    val chatWallpaperValue: Flow<String?>
    val chatWallpaperBlur: Flow<Float>
    val chatMessageCornerRadius: Flow<Int>
    val chatListLayout: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout>
    val chatSwipeGesture: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture>
    val chatMaxMessageChunkSize: Flow<Int>
    val chatFoldersJson: Flow<String?>

    suspend fun setChatFontScale(scale: Float)
    suspend fun setChatThemePreset(preset: ChatThemePreset)
    suspend fun setChatWallpaperType(type: WallpaperType)
    suspend fun setChatWallpaperValue(value: String?)
    suspend fun setChatWallpaperBlur(blur: Float)
    suspend fun setChatMessageCornerRadius(radius: Int)
    suspend fun setChatListLayout(layout: com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout)
    suspend fun setChatSwipeGesture(gesture: com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture)
    suspend fun setChatMaxMessageChunkSize(size: Int)
    suspend fun setChatFoldersJson(json: String)
}

class ChatStoreImpl(private val dataStore: DataStore<Preferences>) : ChatStore {
    override val chatFontScale: Flow<Float> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_FONT_SCALE] ?: SettingsConstants.DEFAULT_CHAT_FONT_SCALE
    }

    override suspend fun setChatFontScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_FONT_SCALE] = scale
        }
    }

    override val chatThemePreset: Flow<ChatThemePreset> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_THEME_PRESET]?.let { value ->
            runCatching { ChatThemePreset.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_CHAT_THEME_PRESET)
        } ?: SettingsConstants.DEFAULT_CHAT_THEME_PRESET
    }

    override suspend fun setChatThemePreset(preset: ChatThemePreset) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_THEME_PRESET] = preset.name
        }
    }

    override val chatWallpaperType: Flow<WallpaperType> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_WALLPAPER_TYPE]?.let { value ->
            runCatching { WallpaperType.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_CHAT_WALLPAPER_TYPE)
        } ?: SettingsConstants.DEFAULT_CHAT_WALLPAPER_TYPE
    }

    override suspend fun setChatWallpaperType(type: WallpaperType) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_WALLPAPER_TYPE] = type.name
        }
    }

    override val chatWallpaperValue: Flow<String?> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_WALLPAPER_VALUE]
    }

    override suspend fun setChatWallpaperValue(value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_VALUE)
                preferences.remove(SettingsConstants.KEY_CHAT_WALLPAPER_BLUR)
            } else {
                preferences[SettingsConstants.KEY_CHAT_WALLPAPER_VALUE] = value
            }
        }
    }

    override val chatWallpaperBlur: Flow<Float> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_WALLPAPER_BLUR] ?: 0f
    }

    override suspend fun setChatWallpaperBlur(blur: Float) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_WALLPAPER_BLUR] = blur
        }
    }

    override val chatMessageCornerRadius: Flow<Int> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_MESSAGE_CORNER_RADIUS] ?: SettingsConstants.DEFAULT_CHAT_MESSAGE_CORNER_RADIUS
    }

    override suspend fun setChatMessageCornerRadius(radius: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_MESSAGE_CORNER_RADIUS] = radius
        }
    }

    override val chatListLayout: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_LIST_LAYOUT]?.let { value ->
            runCatching { com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_CHAT_LIST_LAYOUT)
        } ?: SettingsConstants.DEFAULT_CHAT_LIST_LAYOUT
    }

    override suspend fun setChatListLayout(layout: com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_LIST_LAYOUT] = layout.name
        }
    }

    override val chatSwipeGesture: Flow<com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_SWIPE_GESTURE]?.let { value ->
            runCatching { com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture.valueOf(value) }.getOrDefault(SettingsConstants.DEFAULT_CHAT_SWIPE_GESTURE)
        } ?: SettingsConstants.DEFAULT_CHAT_SWIPE_GESTURE
    }

    override suspend fun setChatSwipeGesture(gesture: com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_SWIPE_GESTURE] = gesture.name
        }
    }

    override val chatMaxMessageChunkSize: Flow<Int> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_MAX_MESSAGE_CHUNK_SIZE] ?: SettingsConstants.DEFAULT_CHAT_MAX_MESSAGE_CHUNK_SIZE
    }

    override suspend fun setChatMaxMessageChunkSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_MAX_MESSAGE_CHUNK_SIZE] = size
        }
    }

    override val chatFoldersJson: Flow<String?> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_CHAT_FOLDERS]
    }

    override suspend fun setChatFoldersJson(json: String) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_CHAT_FOLDERS] = json
        }
    }
}
