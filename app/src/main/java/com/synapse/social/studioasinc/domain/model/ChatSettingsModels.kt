package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.Serializable

enum class ChatThemePreset {
    DEFAULT,
    OCEAN,
    FOREST,
    SUNSET,
    MONOCHROME;

    fun displayName(): String = when (this) {
        DEFAULT -> "Default Purple"
        OCEAN -> "Ocean Blue"
        FOREST -> "Forest Green"
        SUNSET -> "Sunset Orange"
        MONOCHROME -> "Monochrome"
    }
}



enum class WallpaperType {
    DEFAULT,
    SOLID_COLOR,
    PRESET_IMAGE,
    PATTERN
}



data class ChatWallpaper(
    val type: WallpaperType = WallpaperType.DEFAULT,
    val value: String? = null,
    val blurIntensity: Float = 0f
)

enum class ChatListLayout {
    SINGLE_LINE,
    DOUBLE_LINE
}

enum class ChatSwipeGesture {
    ARCHIVE,
    DELETE,
    MUTE,
    PIN,
    READ
}

@Serializable
data class ChatFolder(
    val id: String,
    val name: String,
    val icon: Int? = null,
    val includedChatIds: List<String> = emptyList(),
    val excludedChatIds: List<String> = emptyList(),
    val folderFilters: List<String> = emptyList()
)

data class ChatSettings(
    val fontScale: Float = 1.0f,
    val messageCornerRadius: Int = 16,
    val themePreset: ChatThemePreset = ChatThemePreset.DEFAULT,
    val wallpaperType: WallpaperType = WallpaperType.DEFAULT,
    val wallpaperValue: String? = null,
    val blurIntensity: Float = 0f,
    val listLayout: ChatListLayout = ChatListLayout.DOUBLE_LINE,
    val swipeGesture: ChatSwipeGesture = ChatSwipeGesture.ARCHIVE
)
