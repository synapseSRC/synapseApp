package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import com.synapse.social.studioasinc.settings.ApiKeyInfo
import com.synapse.social.studioasinc.settings.ProviderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiKeySettingsViewModel @Inject constructor(
    private val apiKeySettingsService: ApiKeySettingsService
) : ViewModel() {

    private val _apiKeys = MutableStateFlow<List<ApiKeyInfo>>(emptyList())
    val apiKeys: StateFlow<List<ApiKeyInfo>> = _apiKeys.asStateFlow()

    private val _providerSettings = MutableStateFlow(ProviderSettings())
    val providerSettings: StateFlow<ProviderSettings> = _providerSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apiKeysResult = apiKeySettingsService.loadApiKeys()
                val providerSettingsResult = apiKeySettingsService.loadProviderSettings()
                
                if (apiKeysResult.isSuccess && providerSettingsResult.isSuccess) {
                    _apiKeys.value = apiKeySettingsService.getUserApiKeys()
                    _providerSettings.value = apiKeySettingsService.getProviderSettings()
                } else {
                    _error.value = apiKeysResult.exceptionOrNull()?.message ?: providerSettingsResult.exceptionOrNull()?.message ?: "Failed to load settings"
                }
            } catch (e: Exception) {
                _error.value = "An error occurred while loading settings"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addApiKey(provider: String, keyName: String?, apiKey: String, usageLimit: Int?) {
        if (apiKey.isBlank()) {
            _error.value = "API Key cannot be empty"
            return
        }

        if (usageLimit != null && usageLimit <= 0) {
            _error.value = "Usage limit must be a positive number"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = apiKeySettingsService.storeApiKey(provider, apiKey, keyName, usageLimit)
                if (result.isSuccess) {
                    _apiKeys.value = apiKeySettingsService.getUserApiKeys()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to add API key"
                }
            } catch (e: Exception) {
                _error.value = "An error occurred while adding API key"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteApiKey(keyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = apiKeySettingsService.deleteApiKey(keyId)
                if (result.isSuccess) {
                    _apiKeys.value = apiKeySettingsService.getUserApiKeys()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to delete API key"
                }
            } catch (e: Exception) {
                _error.value = "An error occurred while deleting API key"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePreferredProvider(provider: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiKeySettingsService.updatePreferredProvider(provider)
                _providerSettings.value = apiKeySettingsService.getProviderSettings()
            } catch (e: Exception) {
                 _error.value = "Failed to update preferred provider"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFallbackSetting(fallbackToPlatform: Boolean) {
        viewModelScope.launch {
             _isLoading.value = true
             try {
                 apiKeySettingsService.updateFallbackSetting(fallbackToPlatform)
                 _providerSettings.value = apiKeySettingsService.getProviderSettings()
             } catch (e: Exception) {
                 _error.value = "Failed to update fallback setting"
             } finally {
                 _isLoading.value = false
             }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun getProviderDisplayName(provider: String): String {
        return apiKeySettingsService.getProviderDisplayName(provider)
    }

    fun getAvailableProviders(): List<String> {

        return apiKeySettingsService.getAvailableProviders().filter { it != "platform" }
    }
}
