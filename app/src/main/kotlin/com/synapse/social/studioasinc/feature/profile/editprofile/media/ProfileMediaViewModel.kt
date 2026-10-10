package com.synapse.social.studioasinc.feature.profile.editprofile.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.ImageUtils
import com.synapse.social.studioasinc.core.util.UriUtils
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.presentation.editprofile.UploadState
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileMediaViewModel @Inject constructor(
    application: Application,
    private val repository: EditProfileRepositoryImpl,
    private val settingsRepository: SettingsRepository,
    private val mediaCompressor: MediaCompressor
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProfileMediaUiState())
    val uiState: StateFlow<ProfileMediaUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ProfileMediaNavigation>()
    val navigationEvents: SharedFlow<ProfileMediaNavigation> = _navigationEvents.asSharedFlow()

    private var lastAvatarUri: Uri? = null
    private var lastCoverUri: Uri? = null

    init {
        loadProfileMedia()
    }

    fun loadProfileMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            repository.getUserProfile(userId).collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                avatarUrl = profile.avatar?.ifBlank { null },
                                coverUrl = profile.profileCoverImage?.ifBlank { null }
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun onEvent(event: ProfileMediaEvent) {
        when (event) {
            is ProfileMediaEvent.AvatarSelected -> {
                lastAvatarUri = event.uri
            }
            is ProfileMediaEvent.AvatarCropped -> {
                handleAvatarCropped(event.uri)
            }
            is ProfileMediaEvent.CoverSelected -> {
                lastCoverUri = event.uri
            }
            is ProfileMediaEvent.CoverCropped -> {
                handleCoverCropped(event.uri)
            }
            ProfileMediaEvent.RequestRemoveAvatar -> {
                _uiState.update { it.copy(showRemoveAvatarDialog = true) }
            }
            ProfileMediaEvent.ConfirmRemoveAvatar -> {
                removeAvatar()
            }
            ProfileMediaEvent.DismissRemoveAvatarDialog -> {
                _uiState.update { it.copy(showRemoveAvatarDialog = false) }
            }
            ProfileMediaEvent.RequestRemoveCover -> {
                _uiState.update { it.copy(showRemoveCoverDialog = true) }
            }
            ProfileMediaEvent.ConfirmRemoveCover -> {
                removeCover()
            }
            ProfileMediaEvent.DismissRemoveCoverDialog -> {
                _uiState.update { it.copy(showRemoveCoverDialog = false) }
            }
            ProfileMediaEvent.RetryAvatarUpload -> {
                lastAvatarUri?.let { handleAvatarCropped(it) }
            }
            ProfileMediaEvent.RetryCoverUpload -> {
                lastCoverUri?.let { handleCoverCropped(it) }
            }
            ProfileMediaEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(ProfileMediaNavigation.NavigateBack) }
            }
            ProfileMediaEvent.ViewAvatarHistoryClicked -> {
                viewModelScope.launch { _navigationEvents.emit(ProfileMediaNavigation.NavigateToAvatarHistory) }
            }
            ProfileMediaEvent.ViewCoverHistoryClicked -> {
                viewModelScope.launch { _navigationEvents.emit(ProfileMediaNavigation.NavigateToCoverHistory) }
            }
            ProfileMediaEvent.DismissShareToFeedDialog -> {
                _uiState.update { it.copy(showShareToFeedDialog = false) }
            }
        }
    }

    private fun handleAvatarCropped(uri: Uri) {
        lastAvatarUri = uri
        _uiState.update { it.copy(pendingAvatarUri = uri, avatarUploadState = UploadState.Uploading()) }

        viewModelScope.launch {
            try {
                val uploadResult = compressAndUploadAvatar(uri)
                uploadResult.fold(
                    onSuccess = { url ->
                        _uiState.update {
                            it.copy(
                                avatarUrl = url,
                                avatarUploadState = UploadState.Success,
                                pendingAvatarUri = null,
                                showShareToFeedDialog = true
                            )
                        }
                        val userId = repository.getCurrentUserId()
                        if (userId != null) {
                            val updateData = mapOf("avatar" to url)
                            repository.updateProfile(userId, updateData)
                            repository.addToProfileHistory(userId, url)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(avatarUploadState = UploadState.Error(error.message ?: "Avatar upload failed"))
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(avatarUploadState = UploadState.Error("Unexpected error: ${e.message}"))
                }
            }
        }
    }

    private suspend fun compressAndUploadAvatar(uri: Uri): Result<String> {
        val context = getApplication<Application>()
        var realFilePath = UriUtils.getPathFromUri(context, uri)

        if (realFilePath == null) {
            val tempInputFile = File(context.cacheDir, "temp_input_avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempInputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (tempInputFile.exists() && tempInputFile.length() > 0) {
                realFilePath = tempInputFile.absolutePath
            } else {
                return Result.failure(Exception("Failed to process image content from URI"))
            }
        }

        val sourceFile = File(realFilePath)
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return Result.failure(Exception("Source image file does not exist or is empty"))
        }

        val quality = settingsRepository.mediaUploadQuality.first()
        val compressResult = mediaCompressor.compress(realFilePath, quality)
        val compressedPath = compressResult.getOrNull() ?: realFilePath
        val compressedFile = File(compressedPath)

        if (!compressedFile.exists() || compressedFile.length() == 0L) {
            return Result.failure(Exception("Image compression failed"))
        }

        val userId = repository.getCurrentUserId() ?: return Result.failure(Exception("User not logged in"))
        return repository.uploadAvatar(userId, compressedFile.absolutePath)
    }

    private fun handleCoverCropped(uri: Uri) {
        lastCoverUri = uri
        _uiState.update { it.copy(pendingCoverUri = uri, coverUploadState = UploadState.Uploading()) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                var realFilePath = UriUtils.getPathFromUri(context, uri)

                if (realFilePath == null) {
                    val tempInputFile = File(context.cacheDir, "temp_input_cover_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempInputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (tempInputFile.exists() && tempInputFile.length() > 0) {
                        realFilePath = tempInputFile.absolutePath
                    } else {
                        throw Exception("Failed to process image content from URI")
                    }
                }

                val sourceFile = File(realFilePath)
                if (!sourceFile.exists() || sourceFile.length() == 0L) {
                    throw Exception("Source image file does not exist or is empty")
                }

                val tempFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.jpg")
                ImageUtils.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)

                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("Image compression failed")
                }

                val userId = repository.getCurrentUserId() ?: throw Exception("User not logged in")
                val result = repository.uploadCover(userId, tempFile.absolutePath)

                result.fold(
                    onSuccess = { url ->
                        _uiState.update {
                            it.copy(
                                coverUrl = url,
                                coverUploadState = UploadState.Success,
                                pendingCoverUri = null
                            )
                        }
                        val updateData = mapOf("profile_cover_image" to url)
                        repository.updateProfile(userId, updateData)
                        repository.addToCoverHistory(userId, url)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(coverUploadState = UploadState.Error(error.message ?: "Cover upload failed"))
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(coverUploadState = UploadState.Error("Failed to process cover: ${e.message}"))
                }
            }
        }
    }

    private fun removeAvatar() {
        _uiState.update { it.copy(showRemoveAvatarDialog = false) }
        viewModelScope.launch {
            val userId = repository.getCurrentUserId() ?: return@launch
            val updateData = mapOf<String, Any?>("avatar" to null)
            val result = repository.updateProfile(userId, updateData)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(avatarUrl = null, avatarUploadState = UploadState.Idle) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Failed to remove profile photo: ${error.message}") }
                }
            )
        }
    }

    private fun removeCover() {
        _uiState.update { it.copy(showRemoveCoverDialog = false) }
        viewModelScope.launch {
            val userId = repository.getCurrentUserId() ?: return@launch
            val updateData = mapOf<String, Any?>("profile_cover_image" to null)
            val result = repository.updateProfile(userId, updateData)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(coverUrl = null, coverUploadState = UploadState.Idle) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Failed to remove cover photo: ${error.message}") }
                }
            )
        }
    }
}
