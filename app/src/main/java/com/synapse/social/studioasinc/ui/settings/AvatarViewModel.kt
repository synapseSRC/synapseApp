package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UriUtils
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class AvatarUiState(
    val isUploading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AvatarViewModel @Inject constructor(
    application: Application,
    private val repository: EditProfileRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null, successMessage = null) }
            try {
                val context = getApplication<Application>()
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { it.copy(isUploading = false, error = "User not logged in") }
                    return@launch
                }

                var realFilePath = UriUtils.getPathFromUri(context, uri)
                var tempFile: File? = null

                if (realFilePath == null) {
                    tempFile = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    realFilePath = tempFile.absolutePath
                }

                val uploadResult = repository.uploadAvatar(userId, realFilePath)
                uploadResult.fold(
                    onSuccess = { url ->
                        val updateResult = repository.updateProfile(userId, mapOf("avatar" to url))
                        updateResult.fold(
                            onSuccess = {
                                _uiState.update { it.copy(isUploading = false, successMessage = "Profile photo updated") }
                            },
                            onFailure = { error ->
                                _uiState.update { it.copy(isUploading = false, error = "Failed to update profile: ${error.message}") }
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isUploading = false, error = "Upload failed: ${error.message}") }
                    }
                )

                tempFile?.delete()

            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun uploadBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null, successMessage = null) }
            try {
                val context = getApplication<Application>()
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { it.copy(isUploading = false, error = "User not logged in") }
                    return@launch
                }

                val tempFile = File(context.cacheDir, "temp_avatar_camera_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                val uploadResult = repository.uploadAvatar(userId, tempFile.absolutePath)
                uploadResult.fold(
                    onSuccess = { url ->
                        val updateResult = repository.updateProfile(userId, mapOf("avatar" to url))
                        updateResult.fold(
                            onSuccess = {
                                _uiState.update { it.copy(isUploading = false, successMessage = "Profile photo updated") }
                            },
                            onFailure = { error ->
                                _uiState.update { it.copy(isUploading = false, error = "Failed to update profile: ${error.message}") }
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isUploading = false, error = "Upload failed: ${error.message}") }
                    }
                )

                tempFile.delete()

            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null, successMessage = null) }
            try {
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update { it.copy(isUploading = false, error = "User not logged in") }
                    return@launch
                }

                val updateResult = repository.updateProfile(userId, mapOf("avatar" to "null"))
                updateResult.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isUploading = false, successMessage = "Profile photo removed") }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isUploading = false, error = "Failed to remove photo: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
