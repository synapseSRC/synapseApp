package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.local.database.StorageMigration
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.user.GetUserByIdUseCase
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import com.synapse.social.studioasinc.shared.domain.usecase.GetStorageConfigUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.UpdateStorageProviderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getStorageConfigUseCase: GetStorageConfigUseCase,
    private val updateStorageProviderUseCase: UpdateStorageProviderUseCase,
    private val storageRepository: StorageRepository,
    private val storageMigration: StorageMigration,
    private val authRepository: AuthRepository,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val resetE2EUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.ResetE2EUseCase
) : ViewModel() {

    private val _isResettingE2EE = MutableStateFlow(false)
    val isResettingE2EE: StateFlow<Boolean> = _isResettingE2EE.asStateFlow()

    fun resetE2EE() {
        viewModelScope.launch {
            _isResettingE2EE.value = true
            resetE2EUseCase()
            _isResettingE2EE.value = false
        }
    }

    val storageConfig: StateFlow<StorageConfig> = getStorageConfigUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageConfig()
        )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        fetchCurrentUser()
        viewModelScope.launch {
            storageMigration.migrateIfNeeded()
        }
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                getUserByIdUseCase(userId)
                    .onSuccess { user ->
                        _currentUser.value = user
                    }
                    .onFailure { e ->
                        android.util.Log.e("SettingsViewModel", "Failed to fetch user", e)
                    }
            }
        }
    }

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

    fun updateImgBBConfig(apiKey: String) {
        viewModelScope.launch {
            storageRepository.updateImgBBConfig(apiKey)
        }
    }

    fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String, uploadPreset: String = "") {
        viewModelScope.launch {
            storageRepository.updateCloudinaryConfig(cloudName, apiKey, apiSecret, uploadPreset)
        }
    }

    fun updateR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {
        viewModelScope.launch {
            storageRepository.updateR2Config(accountId, accessKeyId, secretAccessKey, bucketName)
        }
    }

    fun updateSupabaseConfig(url: String, apiKey: String, bucketName: String) {
        viewModelScope.launch {
            storageRepository.updateSupabaseConfig(url, apiKey, bucketName)
        }
    }


    fun updateCompression(enabled: Boolean) {
        viewModelScope.launch {
            storageRepository.updateCompression(enabled)
        }
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
