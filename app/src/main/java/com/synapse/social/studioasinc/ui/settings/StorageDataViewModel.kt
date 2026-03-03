package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn



class StorageDataViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {





    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    private val _dataSaverEnabled = MutableStateFlow(false)
    val dataSaverEnabled: StateFlow<Boolean> = _dataSaverEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _cacheClearedMessage = MutableStateFlow<String?>(null)
    val cacheClearedMessage: StateFlow<String?> = _cacheClearedMessage.asStateFlow()


    val mediaUploadQuality = settingsRepository.mediaUploadQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaUploadQuality.STANDARD)

    val autoDownloadRules = settingsRepository.autoDownloadRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoDownloadRules())

    val useLessDataCalls = settingsRepository.useLessDataCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showMediaQualitySheet = MutableStateFlow(false)
    val showMediaQualitySheet: StateFlow<Boolean> = _showMediaQualitySheet.asStateFlow()

    init {
        loadStorageSettings()
        calculateCacheSize()
    }





    fun setMediaUploadQuality(quality: MediaUploadQuality) {
        viewModelScope.launch {
            settingsRepository.setMediaUploadQuality(quality)
            _showMediaQualitySheet.value = false
        }
    }

    fun openMediaQualitySheet() {
        _showMediaQualitySheet.value = true
    }

    fun closeMediaQualitySheet() {
        _showMediaQualitySheet.value = false
    }

    fun setUseLessDataCalls(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseLessDataCalls(enabled)
        }
    }

    fun setAutoDownloadRule(networkType: String, mediaTypes: Set<MediaType>) {
        viewModelScope.launch {
            settingsRepository.setAutoDownloadRule(networkType, mediaTypes)
        }
    }







    private fun loadStorageSettings() {
        viewModelScope.launch {
            try {

                settingsRepository.cacheSize.collect { size ->
                    _cacheSize.value = size
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to load cache size", e)
            }
        }

        viewModelScope.launch {
            try {

                settingsRepository.dataSaverEnabled.collect { enabled ->
                    _dataSaverEnabled.value = enabled
                }
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to load data saver setting", e)
                _error.value = "Failed to load data saver setting"
            }
        }
    }







    fun calculateCacheSize() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val size = settingsRepository.calculateCacheSize()
                _cacheSize.value = size
                android.util.Log.d(
                    "StorageDataViewModel",
                    "Cache size calculated: ${formatBytes(size)}"
                )
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to calculate cache size", e)
                _error.value = "Failed to calculate cache size"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            _error.value = null
            _cacheClearedMessage.value = null
            try {
                val freedSpace = settingsRepository.clearCache()
                _cacheSize.value = settingsRepository.calculateCacheSize()

                val message = if (freedSpace > 0) {
                    "Cache cleared: ${formatBytes(freedSpace)} freed"
                } else {
                    "Cache was already empty"
                }

                _cacheClearedMessage.value = message

                android.util.Log.d(
                    "StorageDataViewModel",
                    "Cache cleared successfully: ${formatBytes(freedSpace)} freed"
                )
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to clear cache", e)
                _error.value = "Failed to clear cache"
            } finally {
                _isClearingCache.value = false
            }
        }
    }







    fun toggleDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                settingsRepository.setDataSaverEnabled(enabled)
                android.util.Log.d(
                    "StorageDataViewModel",
                    "Data saver ${if (enabled) "enabled" else "disabled"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("StorageDataViewModel", "Failed to toggle data saver", e)
                _error.value = "Failed to update data saver"
            } finally {
                _isLoading.value = false
            }
        }
    }







    fun getStorageStatistics(): StorageStatistics {
        val cacheSizeBytes = _cacheSize.value
        return StorageStatistics(
            cacheSize = cacheSizeBytes,
            cacheSizeFormatted = formatBytes(cacheSizeBytes)
        )
    }







    fun navigateToStorageProviderConfig() {
        android.util.Log.d("StorageDataViewModel", "Navigate to storage provider configuration")

    }







    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }



    fun clearError() {
        _error.value = null
    }



    fun clearCacheClearedMessage() {
        _cacheClearedMessage.value = null
    }
}



data class StorageStatistics(
    val cacheSize: Long,
    val cacheSizeFormatted: String
)
