package com.synapse.social.studioasinc.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AvatarViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    fun uploadPhoto(uri: Uri) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null, successMessage = null)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val userId = editProfileRepository.getCurrentUserId() ?: run {
                    _uiState.value = _uiState.value.copy(isUploading = false)
                    return@withContext
                }
                val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Could not read the selected image."
                    )
                    return@withContext
                }
                val tempFile = File.createTempFile("avatar_", ".tmp", context.cacheDir)
                try {
                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val uploadResult = editProfileRepository.uploadAvatar(userId, tempFile.absolutePath)
                    uploadResult.fold(
                        onSuccess = { mediaReference ->
                            val updateResult = editProfileRepository.updateProfile(
                                userId,
                                mapOf("avatar" to mediaReference)
                            )
                            updateResult.fold(
                                onSuccess = {
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        currentAvatarUrl = mediaReference,
                                        successMessage = "Photo uploaded"
                                    )
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        error = "Avatar uploaded but could not be saved to profile: ${error.message ?: "unknown error"}"
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                error = "Avatar upload failed: ${error.message ?: "unknown error"}"
                            )
                        }
                    )
                } finally {
                    tempFile.delete()
                }
            }
        }
    }

    fun uploadBitmap(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(isUploading = true, error = null, successMessage = null)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val userId = editProfileRepository.getCurrentUserId() ?: run {
                    _uiState.value = _uiState.value.copy(isUploading = false)
                    return@withContext
                }
                val tempFile = saveBitmapToTempFile(bitmap, "avatar_") ?: run {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Could not process the captured image."
                    )
                    return@withContext
                }
                try {
                    val uploadResult = editProfileRepository.uploadAvatar(userId, tempFile.absolutePath)
                    uploadResult.fold(
                        onSuccess = { mediaReference ->
                            val updateResult = editProfileRepository.updateProfile(
                                userId,
                                mapOf("avatar" to mediaReference)
                            )
                            updateResult.fold(
                                onSuccess = {
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        currentAvatarUrl = mediaReference,
                                        successMessage = "Photo uploaded"
                                    )
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        error = "Avatar uploaded but could not be saved to profile: ${error.message ?: "unknown error"}"
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                error = "Avatar upload failed: ${error.message ?: "unknown error"}"
                            )
                        }
                    )
                } finally {
                    tempFile.delete()
                }
            }
        }
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap, prefix: String): File? {
        return try {
            val tempFile = File.createTempFile(prefix, ".jpg", context.cacheDir)
            tempFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            tempFile
        } catch (e: Exception) {
            null
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
