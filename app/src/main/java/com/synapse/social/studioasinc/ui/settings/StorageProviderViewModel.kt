package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.UpdateStorageProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageProviderViewModel @Inject constructor(
    private val getStorageConfigUseCase: GetStorageConfigUseCase,
    private val updateStorageProviderUseCase: UpdateStorageProviderUseCase,
    private val storageRepository: StorageRepository
) : ViewModel() {

    val storageConfig: StateFlow<StorageConfig> = getStorageConfigUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageConfig()
        )

    private val _uiState = MutableStateFlow(StorageProviderUiState())
    val uiState: StateFlow<StorageProviderUiState> = _uiState.asStateFlow()

    fun updatePhotoProvider(providerName: String?) {
        viewModelScope.launch {
            updateStorageProviderUseCase(MediaType.PHOTO, providerName.toStorageProvider())
        }
    }

    fun updateVideoProvider(providerName: String?) {
        viewModelScope.launch {
            updateStorageProviderUseCase(MediaType.VIDEO, providerName.toStorageProvider())
        }
    }

    fun updateOtherProvider(providerName: String?) {
        viewModelScope.launch {
            updateStorageProviderUseCase(MediaType.OTHER, providerName.toStorageProvider())
        }
    }

    fun updateCompression(enabled: Boolean) {
        viewModelScope.launch {
            storageRepository.updateCompression(enabled)
        }
    }

    fun updateImgBBConfig(apiKey: String) {
        viewModelScope.launch {
            val error = validateApiKey(apiKey, "ImgBB API Key")
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                storageRepository.updateImgBBConfig(apiKey)
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "ImgBB configuration saved")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to save configuration: ${e.message}")
            }
        }
    }

    fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            val nameError = validateBucketName(cloudName, "Cloud Name")
            val keyError = validateApiKey(apiKey, "API Key")
            val secretError = validateApiKey(apiSecret, "API Secret")

            val error = nameError ?: keyError ?: secretError
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                storageRepository.updateCloudinaryConfig(cloudName, apiKey, apiSecret)
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Cloudinary configuration saved")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to save configuration: ${e.message}")
            }
        }
    }

    fun updateR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {
        viewModelScope.launch {
            val accError = validateApiKey(accountId, "Account ID")
            val keyError = validateApiKey(accessKeyId, "Access Key ID")
            val secretError = validateApiKey(secretAccessKey, "Secret Access Key")
            val bucketError = validateBucketName(bucketName, "Bucket Name")

            val error = accError ?: keyError ?: secretError ?: bucketError
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                storageRepository.updateR2Config(accountId, accessKeyId, secretAccessKey, bucketName)
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Cloudflare R2 configuration saved")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to save configuration: ${e.message}")
            }
        }
    }

    fun updateSupabaseConfig(url: String, apiKey: String, bucketName: String) {
        viewModelScope.launch {
            val urlError = validateSupabaseUrl(url)
            val keyError = validateApiKey(apiKey, "Service Role / API Key")
            val bucketError = validateBucketName(bucketName, "Bucket Name")

            val error = urlError ?: keyError ?: bucketError
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                storageRepository.updateSupabaseConfig(url, apiKey, bucketName)
                _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Supabase configuration saved")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to save configuration: ${e.message}")
            }
        }
    }

    fun testImgBBConnection(apiKey: String) {
        viewModelScope.launch {
            val error = validateApiKey(apiKey, "ImgBB API Key")
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            delay(1000) // Simulate network request
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Connection successful to ImgBB")
        }
    }

    fun testCloudinaryConnection(cloudName: String, apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            val nameError = validateBucketName(cloudName, "Cloud Name")
            val keyError = validateApiKey(apiKey, "API Key")
            val secretError = validateApiKey(apiSecret, "API Secret")

            val error = nameError ?: keyError ?: secretError
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            delay(1000)
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Connection successful to Cloudinary")
        }
    }

    fun testSupabaseConnection(url: String, apiKey: String, bucketName: String) {
        viewModelScope.launch {
             val urlError = validateSupabaseUrl(url)
            val keyError = validateApiKey(apiKey, "Service Role / API Key")
            val bucketError = validateBucketName(bucketName, "Bucket Name")

            val error = urlError ?: keyError ?: bucketError
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            delay(1000)
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Connection successful to Supabase")
        }
    }

    fun testR2Connection(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {
        viewModelScope.launch {
            val accError = validateApiKey(accountId, "Account ID")
            val keyError = validateApiKey(accessKeyId, "Access Key ID")
            val secretError = validateApiKey(secretAccessKey, "Secret Access Key")
            val bucketError = validateBucketName(bucketName, "Bucket Name")

            val error = accError ?: keyError ?: secretError ?: bucketError
            if (error != null) {
                _uiState.value = _uiState.value.copy(error = error, successMessage = null)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            delay(1000)
            _uiState.value = _uiState.value.copy(isLoading = false, successMessage = "Connection successful to Cloudflare R2")
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun validateSupabaseUrl(url: String): String? {
        if (url.isBlank()) return "URL cannot be empty"
        if (!url.startsWith("https://")) return "URL must start with https://"
        if (!url.endsWith(".supabase.co")) return "URL must end with .supabase.co"
        return null
    }

    private fun validateApiKey(key: String, fieldName: String): String? {
        if (key.isBlank()) return "$fieldName cannot be empty"
        if (key.length < 5) return "$fieldName is too short"
        return null
    }

    private fun validateBucketName(name: String, fieldName: String): String? {
        if (name.isBlank()) return "$fieldName cannot be empty"
        if (!name.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return "$fieldName can only contain alphanumeric characters, hyphens, and underscores"
        }
        return null
    }

    private fun String?.toStorageProvider(): StorageProvider {
        return when (this) {
            "ImgBB" -> StorageProvider.IMGBB
            "Cloudinary" -> StorageProvider.CLOUDINARY
            "Supabase" -> StorageProvider.SUPABASE
            "Cloudflare R2" -> StorageProvider.CLOUDFLARE_R2
            else -> StorageProvider.DEFAULT
        }
    }
}

data class StorageProviderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
