package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



class AppearanceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepositoryImpl.getInstance(application)





    private val _appearanceSettings = MutableStateFlow(AppearanceSettings())
    val appearanceSettings: StateFlow<AppearanceSettings> = _appearanceSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()



    val isDynamicColorSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    init {
        loadAppearanceSettings()
    }







    private fun loadAppearanceSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.appearanceSettings.collect { settings ->
                    _appearanceSettings.value = settings
                }
            } catch (e: kotlinx.coroutines.CancellationException) {

                throw e
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to load appearance settings", e)
                _error.value = "Failed to load appearance settings"
            }
        }
    }







    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setThemeMode(mode)

                com.synapse.social.studioasinc.feature.shared.theme.ThemeManager.applyThemeMode(mode)
                android.util.Log.d("AppearanceViewModel", "Theme mode set to: $mode")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to set theme mode", e)
                _error.value = "Failed to update theme mode"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun getThemeModeDisplayName(mode: ThemeMode): String = when (mode) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System Default"
    }



    fun getThemeModeOptions(): List<String> = listOf(
        getThemeModeDisplayName(ThemeMode.LIGHT),
        getThemeModeDisplayName(ThemeMode.DARK),
        getThemeModeDisplayName(ThemeMode.SYSTEM)
    )



    fun getThemeModeFromDisplayName(displayName: String): ThemeMode = when (displayName) {
        "Light" -> ThemeMode.LIGHT
        "Dark" -> ThemeMode.DARK
        "System Default" -> ThemeMode.SYSTEM
        else -> ThemeMode.SYSTEM
    }







    fun setDynamicColorEnabled(enabled: Boolean) {
        if (!isDynamicColorSupported) {
            android.util.Log.w("AppearanceViewModel", "Dynamic color not supported on this device")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setDynamicColorEnabled(enabled)
                android.util.Log.d("AppearanceViewModel", "Dynamic color ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to toggle dynamic color", e)
                _error.value = "Failed to update dynamic color"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun setFontScale(scale: FontScale) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setFontScale(scale)
                android.util.Log.d("AppearanceViewModel", "Font scale set to: $scale")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to set font scale", e)
                _error.value = "Failed to update font scale"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun getFontScaleFromSliderValue(value: Float): FontScale = when (value.toInt()) {
        0 -> FontScale.SMALL
        1 -> FontScale.MEDIUM
        2 -> FontScale.LARGE
        3 -> FontScale.EXTRA_LARGE
        else -> FontScale.MEDIUM
    }



    fun getSliderValueFromFontScale(scale: FontScale): Float = when (scale) {
        FontScale.SMALL -> 0f
        FontScale.MEDIUM -> 1f
        FontScale.LARGE -> 2f
        FontScale.EXTRA_LARGE -> 3f
    }



    fun getFontScalePreviewText(scale: FontScale): String = scale.displayName()







    fun setPostViewStyle(style: PostViewStyle) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setPostViewStyle(style)
                android.util.Log.d("AppearanceViewModel", "Post view style set to: $style")
            } catch (e: Exception) {
                android.util.Log.e("AppearanceViewModel", "Failed to set post view style", e)
                _error.value = "Failed to update post view style"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun getPostViewStyleOptions(): List<String> = listOf(
        PostViewStyle.SWIPE.displayName(),
        PostViewStyle.GRID.displayName()
    )



    fun getPostViewStyleFromDisplayName(displayName: String): PostViewStyle = when (displayName) {
        "Swipe" -> PostViewStyle.SWIPE
        "Grid" -> PostViewStyle.GRID
        else -> PostViewStyle.SWIPE
    }







    fun navigateToChatCustomization() {
        android.util.Log.d("AppearanceViewModel", "Navigate to chat customization")

    }







    fun clearError() {
        _error.value = null
    }
}
