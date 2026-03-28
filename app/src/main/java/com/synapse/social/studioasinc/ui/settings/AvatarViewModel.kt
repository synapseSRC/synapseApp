package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AvatarViewModel @Inject constructor(
    private val editProfileRepository: EditProfileRepositoryImpl
) : ViewModel() {

    data class UiState(
        val currentAvatarUrl: String? = null,
        val isUploading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRemoving = MutableStateFlow(false)
    val isRemoving: StateFlow<Boolean> = _isRemoving.asStateFlow()

    init {
        loadCurrentAvatar()
    }

    private fun loadCurrentAvatar() {
        viewModelScope.launch {
            try {
                val userId = editProfileRepository.getCurrentUserId()
                if (userId != null) {
                    editProfileRepository.getUserProfile(userId).collect { result ->
                        result.fold(
                            onSuccess = { profile ->
                                _uiState.value = _uiState.value.copy(
                                    currentAvatarUrl = profile.avatar
                                )
                            },
                            onFailure = { }
                        )
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun uploadPhoto(uri: android.net.Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true)
        // Simulate upload
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _uiState.value = _uiState.value.copy(isUploading = false, successMessage = "Photo uploaded")
        }
    }

    fun uploadBitmap(bitmap: android.graphics.Bitmap) {
        _uiState.value = _uiState.value.copy(isUploading = true)
        // Simulate upload
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _uiState.value = _uiState.value.copy(isUploading = false, successMessage = "Photo uploaded")
        }
    }

    fun removeProfilePhoto(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isRemoving.value = true
            try {
                val userId = editProfileRepository.getCurrentUserId()
                if (userId != null) {
                    val updateData = mapOf("avatar" to "")
                    val result = editProfileRepository.updateProfile(userId, updateData)
                    result.fold(
                        onSuccess = { onSuccess() },
                        onFailure = { error -> onError(error.message ?: "Failed to remove profile photo") }
                    )
                } else {
                    onError("User not logged in")
                }
            } catch (e: Exception) {
                onError(e.message ?: "An unexpected error occurred")
            } finally {
                _isRemoving.value = false
            }
        }
    }
}
