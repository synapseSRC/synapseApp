package com.synapse.social.studioasinc.feature.profile.editprofile.appearance

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
class ProfileAppearanceViewModel @Inject constructor(
    application: Application,
    private val repository: EditProfileRepositoryImpl,
    private val settingsRepository: SettingsRepository,
    private val mediaCompressor: MediaCompressor
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProfileAppearanceUiState())
    val uiState: StateFlow<ProfileAppearanceUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ProfileAppearanceNavigation>()
    val navigationEvents: SharedFlow<ProfileAppearanceNavigation> = _navigationEvents.asSharedFlow()

    private var lastCoverUri: Uri? = null

    init {
        loadProfileAppearance()
    }

    fun loadProfileAppearance() {
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
                                profile = profile,
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

    fun onEvent(event: ProfileAppearanceEvent) {
        when (event) {
            is ProfileAppearanceEvent.CoverSelected -> {
                lastCoverUri = event.uri
            }
            is ProfileAppearanceEvent.CoverCropped -> {
                lastCoverUri = event.uri
                _uiState.update {
                    it.copy(
                        pendingCoverUri = event.uri,
                        hasPendingChanges = true,
                        uploadState = UploadState.Idle
                    )
                }
            }
            ProfileAppearanceEvent.ResetChanges -> {
                lastCoverUri = null
                _uiState.update {
                    it.copy(
                        pendingCoverUri = null,
                        hasPendingChanges = false,
                        uploadState = UploadState.Idle
                    )
                }
            }
            ProfileAppearanceEvent.ApplyChanges -> {
                applyChanges()
            }
            ProfileAppearanceEvent.RequestRemoveCover -> {
                _uiState.update { it.copy(showRemoveCoverDialog = true) }
            }
            ProfileAppearanceEvent.ConfirmRemoveCover -> {
                removeCover()
            }
            ProfileAppearanceEvent.DismissRemoveCoverDialog -> {
                _uiState.update { it.copy(showRemoveCoverDialog = false) }
            }
            ProfileAppearanceEvent.RetryCoverUpload -> {
                applyChanges()
            }
            ProfileAppearanceEvent.BackClicked -> {
                if (_uiState.value.hasPendingChanges) {
                    _uiState.update { it.copy(showUnsavedChangesDialog = true) }
                } else {
                    viewModelScope.launch { _navigationEvents.emit(ProfileAppearanceNavigation.NavigateBack) }
                }
            }
            ProfileAppearanceEvent.ConfirmDiscardUnsaved -> {
                _uiState.update { it.copy(showUnsavedChangesDialog = false, pendingCoverUri = null, hasPendingChanges = false) }
                viewModelScope.launch { _navigationEvents.emit(ProfileAppearanceNavigation.NavigateBack) }
            }
            ProfileAppearanceEvent.DismissUnsavedDialog -> {
                _uiState.update { it.copy(showUnsavedChangesDialog = false) }
            }
        }
    }

    private fun applyChanges() {
        val pendingUri = _uiState.value.pendingCoverUri ?: return

        _uiState.update { it.copy(isSaving = true, uploadState = UploadState.Uploading()) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                var realFilePath = UriUtils.getPathFromUri(context, pendingUri)

                if (realFilePath == null) {
                    val tempInputFile = File(context.cacheDir, "temp_input_cover_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(pendingUri)?.use { inputStream ->
                        tempInputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (tempInputFile.exists() && tempInputFile.length() > 0) {
                        realFilePath = tempInputFile.absolutePath
                    } else {
                        throw Exception("Failed to process cover image from URI")
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
                        val updateData = mapOf("profile_cover_image" to url)
                        repository.updateProfile(userId, updateData)
                        repository.addToCoverHistory(userId, url)

                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                coverUrl = url,
                                pendingCoverUri = null,
                                hasPendingChanges = false,
                                uploadState = UploadState.Success
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                uploadState = UploadState.Error(error.message ?: "Cover upload failed")
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        uploadState = UploadState.Error("Failed to process cover: ${e.message}")
                    )
                }
            }
        }
    }

    private fun removeCover() {
        _uiState.update { it.copy(showRemoveCoverDialog = false, isSaving = true) }
        viewModelScope.launch {
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            val updateData = mapOf<String, Any?>("profile_cover_image" to null)
            val result = repository.updateProfile(userId, updateData)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            coverUrl = null,
                            pendingCoverUri = null,
                            hasPendingChanges = false,
                            uploadState = UploadState.Idle
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = "Failed to remove background: ${error.message}")
                    }
                }
            )
        }
    }
}
