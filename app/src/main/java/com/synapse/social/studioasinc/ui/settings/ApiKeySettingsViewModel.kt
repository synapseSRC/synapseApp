package com.synapse.social.studioasinc.ui.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import com.synapse.social.studioasinc.settings.ApiKeyInfo
import com.synapse.social.studioasinc.settings.ProviderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiKeySettingsViewModel @Inject constructor(
    private val apiKeySettingsService: ApiKeySettingsService
) : ViewModel() {

    private val _apiKeys = mutableStateOf<List<ApiKeyInfo>>(emptyList())
    val apiKeys: State<List<ApiKeyInfo>> = _apiKeys

    private val _providerSettings = mutableStateOf(ProviderSettings())
    val providerSettings: State<ProviderSettings> = _providerSettings

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiKeySettingsService.loadApiKeys()
                apiKeySettingsService.loadProviderSettings()
                
                _apiKeys.value = apiKeySettingsService.getUserApiKeys()
                _providerSettings.value = apiKeySettingsService.getProviderSettings()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addApiKey(provider: String, keyName: String, apiKey: String) {
        viewModelScope.launch {
            apiKeySettingsService.addApiKey(provider, keyName, apiKey)
            loadSettings()
        }
    }

    fun deleteApiKey(keyId: String) {
        viewModelScope.launch {
            apiKeySettingsService.deleteApiKey(keyId)
            loadSettings()
        }
    }

    fun updatePreferredProvider(provider: String) {
        viewModelScope.launch {
            apiKeySettingsService.updatePreferredProvider(provider)
            loadSettings()
        }
    }

    fun updateFallbackSetting(fallbackToPlatform: Boolean) {
        viewModelScope.launch {
            apiKeySettingsService.updateFallbackSetting(fallbackToPlatform)
            loadSettings()
        }
    }

    fun getProviderDisplayName(provider: String): String {
        return apiKeySettingsService.getProviderDisplayName(provider)
    }

    fun getAvailableProviders(): List<String> {

        return apiKeySettingsService.getAvailableProviders().filter { it != "platform" }
    }
}
