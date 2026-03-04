package com.synapse.social.studioasinc.presentation.editprofile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.util.UriUtils
import com.synapse.social.studioasinc.core.util.ImageUtils
import com.synapse.social.studioasinc.domain.model.Gender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    application: Application,
    private val repository: EditProfileRepository
) : AndroidViewModel(application) {



    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<EditProfileNavigation>()
    val navigationEvents: SharedFlow<EditProfileNavigation> = _navigationEvents.asSharedFlow()

    private var usernameValidationJob: Job? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
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
                                username = profile.username,
                                nickname = profile.displayName ?: "",
                                bio = profile.bio ?: "",
                                avatarUrl = profile.avatar,
                                coverUrl = profile.profileCoverImage,
                                selectedGender = profile.safeGender,
                                selectedRegion = profile.region.takeIf { it != "null" },
                                currentCity = profile.currentCity ?: "",
                                hometown = profile.hometown ?: "",
                                occupation = profile.occupation ?: "",
                                workplace = profile.workplace ?: "",
                                education = profile.education ?: "",
                                pronouns = profile.pronouns ?: "",
                                birthday = profile.birthday ?: "",
                                relationshipStatus = profile.relationshipStatus ?: "",
                                discordTag = profile.discordTag ?: "",
                                githubProfile = profile.githubProfile ?: "",
                                personalWebsite = profile.personalWebsite ?: "",
                                publicEmail = profile.publicEmail ?: ""
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

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.UsernameChanged -> {
                _uiState.update { it.copy(username = event.username, hasChanges = true) }
                validateUsername(event.username)
            }
            is EditProfileEvent.NicknameChanged -> {
                _uiState.update { it.copy(nickname = event.nickname, hasChanges = true) }
                validateNickname(event.nickname)
            }
            is EditProfileEvent.BiographyChanged -> {
                _uiState.update { it.copy(bio = event.bio, hasChanges = true) }
                validateBiography(event.bio)
            }
            is EditProfileEvent.GenderSelected -> {
                _uiState.update { it.copy(selectedGender = event.gender, hasChanges = true) }
            }
            is EditProfileEvent.RegionSelected -> {
                _uiState.update { it.copy(selectedRegion = event.region, hasChanges = true) }
            }
            is EditProfileEvent.AvatarSelected -> {
                handleAvatarSelection(event.uri)
            }
            is EditProfileEvent.CoverSelected -> {
                handleCoverSelection(event.uri)
            }
            EditProfileEvent.RetryAvatarUpload -> {
                retryAvatarUpload()
            }
            EditProfileEvent.RetryCoverUpload -> {
                retryCoverUpload()
            }
            EditProfileEvent.SaveClicked -> {
                saveProfile()
            }
            EditProfileEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(EditProfileNavigation.NavigateBack) }
            }
            EditProfileEvent.ProfileHistoryClicked -> {
                viewModelScope.launch { _navigationEvents.emit(EditProfileNavigation.NavigateToProfileHistory) }
            }
            EditProfileEvent.CoverHistoryClicked -> {
                viewModelScope.launch { _navigationEvents.emit(EditProfileNavigation.NavigateToCoverHistory) }
            }
            is EditProfileEvent.CurrentCityChanged -> _uiState.update { it.copy(currentCity = event.city, hasChanges = true) }
            is EditProfileEvent.HometownChanged -> _uiState.update { it.copy(hometown = event.hometown, hasChanges = true) }
            is EditProfileEvent.OccupationChanged -> _uiState.update { it.copy(occupation = event.occupation, hasChanges = true) }
            is EditProfileEvent.WorkplaceChanged -> _uiState.update { it.copy(workplace = event.workplace, hasChanges = true) }
            is EditProfileEvent.EducationChanged -> _uiState.update { it.copy(education = event.education, hasChanges = true) }
            is EditProfileEvent.PronounsChanged -> _uiState.update { it.copy(pronouns = event.pronouns, hasChanges = true) }
            is EditProfileEvent.BirthdayChanged -> _uiState.update { it.copy(birthday = event.birthday, hasChanges = true) }
            is EditProfileEvent.RelationshipStatusChanged -> _uiState.update { it.copy(relationshipStatus = event.status, hasChanges = true) }
            is EditProfileEvent.DiscordTagChanged -> _uiState.update { it.copy(discordTag = event.tag, hasChanges = true) }
            is EditProfileEvent.GithubProfileChanged -> _uiState.update { it.copy(githubProfile = event.profile, hasChanges = true) }
            is EditProfileEvent.PersonalWebsiteChanged -> _uiState.update { it.copy(personalWebsite = event.website, hasChanges = true) }
            is EditProfileEvent.PublicEmailChanged -> _uiState.update { it.copy(publicEmail = event.email, hasChanges = true) }
        }
    }

    private fun validateUsername(username: String) {
        usernameValidationJob?.cancel()

        if (username.isEmpty()) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username is required")) }
            return
        }


        if (!username.matches(Regex("[a-z0-9_.]+"))) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Only lowercase letters, numbers, _ and . allowed")) }
            return
        }
        if (!username.first().isLetter()) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username must start with a letter")) }
            return
        }
        if (username.length < 3) {
            _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username must be at least 3 characters")) }
            return
        }
        if (username.length > 25) {
             _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username max 25 characters")) }
             return
        }

        _uiState.update { it.copy(usernameValidation = UsernameValidation.Checking) }

        usernameValidationJob = viewModelScope.launch {
            delay(500)
            val userId = repository.getCurrentUserId() ?: return@launch


            if (username == _uiState.value.profile?.username) {
                 _uiState.update { it.copy(usernameValidation = UsernameValidation.Valid) }
                 return@launch
            }

            val result = repository.checkUsernameAvailability(username, userId)
            result.fold(
                onSuccess = { isAvailable ->
                    if (isAvailable) {
                        _uiState.update { it.copy(usernameValidation = UsernameValidation.Valid) }
                    } else {
                        _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Username is already taken")) }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(usernameValidation = UsernameValidation.Error("Failed to check availability")) }
                }
            )
        }
    }

    private fun validateNickname(nickname: String) {
        if (nickname.length > 30) {
            _uiState.update { it.copy(nicknameError = "Nickname must be 30 characters or less") }
        } else {
            _uiState.update { it.copy(nicknameError = null) }
        }
    }

    private fun validateBiography(bio: String) {
        if (bio.length > 250) {
            _uiState.update { it.copy(bioError = "Bio must be 250 characters or less") }
        } else {
            _uiState.update { it.copy(bioError = null) }
        }
    }

    private var lastAvatarUri: Uri? = null
    private var lastCoverUri: Uri? = null

    private fun handleAvatarSelection(uri: Uri) {
        lastAvatarUri = uri
        _uiState.update { it.copy(avatarUploadState = UploadState.Uploading()) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                android.util.Log.d("EditProfile", "Processing avatar URI: $uri")


                var realFilePath = UriUtils.getPathFromUri(context, uri)
                android.util.Log.d("EditProfile", "Converted file path: $realFilePath")


                if (realFilePath == null) {
                    android.util.Log.d("EditProfile", "URI conversion failed, copying content to temp file")
                    val tempInputFile = File(context.cacheDir, "temp_input_avatar_${System.currentTimeMillis()}.jpg")

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempInputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (tempInputFile.exists() && tempInputFile.length() > 0) {
                        realFilePath = tempInputFile.absolutePath
                        android.util.Log.d("EditProfile", "Successfully copied to temp file: $realFilePath")
                    } else {
                        throw Exception("Failed to copy image content from URI")
                    }
                }


                val sourceFile = File(realFilePath)
                if (!sourceFile.exists()) {
                    throw Exception("Source file does not exist: $realFilePath")
                }
                if (sourceFile.length() == 0L) {
                    throw Exception("Source file is empty: $realFilePath")
                }


                val tempFile = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
                android.util.Log.d("EditProfile", "Compressing image to: ${tempFile.absolutePath}")

                ImageUtils.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)


                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("Image compression failed")
                }

                android.util.Log.d("EditProfile", "Image compressed successfully, size: ${tempFile.length()} bytes")


                uploadAvatar(tempFile.absolutePath)

            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Avatar processing failed", e)
                _uiState.update {
                    it.copy(avatarUploadState = UploadState.Error("Failed to process image: ${e.message}"))
                }
            }
        }
    }

    private fun uploadAvatar(filePath: String) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(avatarUploadState = UploadState.Error("User not logged in"))
                    }
                    return@launch
                }

                android.util.Log.d("EditProfile", "Starting avatar upload for user: $userId, file: $filePath")
                _uiState.update { it.copy(avatarUploadState = UploadState.Uploading()) }

                val result = repository.uploadAvatar(userId, filePath)
                result.fold(
                    onSuccess = { url ->
                        android.util.Log.d("EditProfile", "Avatar upload successful: $url")
                        _uiState.update {
                            it.copy(
                                avatarUrl = url,
                                avatarUploadState = UploadState.Success,
                                hasChanges = true
                            )
                        }

                        viewModelScope.launch {
                            try {
                                repository.addToProfileHistory(userId, url)
                            } catch (e: Exception) {
                                android.util.Log.w("EditProfile", "Failed to add to profile history", e)
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("EditProfile", "Avatar upload failed", error)
                        _uiState.update {
                            it.copy(avatarUploadState = UploadState.Error("Avatar upload failed: ${error.message}"))
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Unexpected error during avatar upload", e)
                _uiState.update {
                    it.copy(avatarUploadState = UploadState.Error("Unexpected error: ${e.message}"))
                }
            }
        }
    }

    private fun handleCoverSelection(uri: Uri) {
        lastCoverUri = uri
        _uiState.update { it.copy(coverUploadState = UploadState.Uploading()) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                android.util.Log.d("EditProfile", "Processing cover URI: $uri")


                var realFilePath = UriUtils.getPathFromUri(context, uri)
                android.util.Log.d("EditProfile", "Converted file path: $realFilePath")


                if (realFilePath == null) {
                    android.util.Log.d("EditProfile", "URI conversion failed, copying content to temp file")
                    val tempInputFile = File(context.cacheDir, "temp_input_cover_${System.currentTimeMillis()}.jpg")

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        tempInputFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (tempInputFile.exists() && tempInputFile.length() > 0) {
                        realFilePath = tempInputFile.absolutePath
                        android.util.Log.d("EditProfile", "Successfully copied to temp file: $realFilePath")
                    } else {
                        throw Exception("Failed to copy image content from URI")
                    }
                }


                val sourceFile = File(realFilePath)
                if (!sourceFile.exists()) {
                    throw Exception("Source file does not exist: $realFilePath")
                }
                if (sourceFile.length() == 0L) {
                    throw Exception("Source file is empty: $realFilePath")
                }


                val tempFile = File(context.cacheDir, "temp_cover_${System.currentTimeMillis()}.jpg")
                android.util.Log.d("EditProfile", "Compressing image to: ${tempFile.absolutePath}")

                ImageUtils.resizeBitmapFileRetainRatio(realFilePath, tempFile.absolutePath, 1024)


                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw Exception("Image compression failed")
                }

                android.util.Log.d("EditProfile", "Image compressed successfully, size: ${tempFile.length()} bytes")


                uploadCover(tempFile.absolutePath)

            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Cover processing failed", e)
                _uiState.update {
                    it.copy(coverUploadState = UploadState.Error("Failed to process image: ${e.message}"))
                }
            }
        }
    }

    private fun uploadCover(filePath: String) {
        viewModelScope.launch {
            try {
                val userId = repository.getCurrentUserId()
                if (userId == null) {
                    _uiState.update {
                        it.copy(coverUploadState = UploadState.Error("User not logged in"))
                    }
                    return@launch
                }

                android.util.Log.d("EditProfile", "Starting cover upload for user: $userId, file: $filePath")
                _uiState.update { it.copy(coverUploadState = UploadState.Uploading()) }

                val result = repository.uploadCover(userId, filePath)
                result.fold(
                    onSuccess = { url ->
                        android.util.Log.d("EditProfile", "Cover upload successful: $url")
                        _uiState.update {
                            it.copy(
                                coverUrl = url,
                                coverUploadState = UploadState.Success,
                                hasChanges = true
                            )
                        }

                        viewModelScope.launch {
                            try {
                                repository.addToCoverHistory(userId, url)
                            } catch (e: Exception) {
                                android.util.Log.w("EditProfile", "Failed to add to cover history", e)
                            }
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("EditProfile", "Cover upload failed", error)
                        _uiState.update {
                            it.copy(coverUploadState = UploadState.Error("Cover upload failed: ${error.message}"))
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Unexpected error during cover upload", e)
                _uiState.update {
                    it.copy(coverUploadState = UploadState.Error("Unexpected error: ${e.message}"))
                }
            }
        }
    }

    private fun retryAvatarUpload() {
        lastAvatarUri?.let { uri ->
            handleAvatarSelection(uri)
        }
    }

    private fun retryCoverUpload() {
        lastCoverUri?.let { uri ->
            handleCoverSelection(uri)
        }
    }

    private fun saveProfile() {
        val state = _uiState.value

        if (state.usernameValidation is UsernameValidation.Error || state.nicknameError != null || state.bioError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = repository.getCurrentUserId()

            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            try {
                val updateData = mutableMapOf<String, String>()

                updateData["username"] = state.username
                updateData["display_name"] = state.nickname
                updateData["bio"] = state.bio
                updateData["gender"] = state.selectedGender.name.lowercase()

                state.selectedRegion?.let { updateData["region"] = it }
                state.avatarUrl?.let { updateData["avatar"] = it }
                state.coverUrl?.let { updateData["profile_cover_image"] = it }
                
                updateData["current_city"] = state.currentCity
                updateData["hometown"] = state.hometown
                updateData["occupation"] = state.occupation
                updateData["workplace"] = state.workplace
                updateData["education"] = state.education
                updateData["pronouns"] = state.pronouns
                updateData["birthday"] = state.birthday
                updateData["relationship_status"] = state.relationshipStatus
                updateData["discord_tag"] = state.discordTag
                updateData["github_profile"] = state.githubProfile
                updateData["personal_website"] = state.personalWebsite
                updateData["public_email"] = state.publicEmail

                val result = repository.updateProfile(userId, updateData)

                result.fold(
                    onSuccess = {
                         val originalUsername = state.profile?.username
                         if (originalUsername != null && originalUsername != state.username) {
                             val syncResult = repository.syncUsernameChange(originalUsername, state.username, userId)
                             syncResult.fold(
                                 onSuccess = {
                                     _uiState.update { it.copy(isSaving = false, hasChanges = false) }
                                     _navigationEvents.emit(EditProfileNavigation.NavigateBack)
                                 },
                                 onFailure = { error ->
                                     _uiState.update { it.copy(isSaving = false, error = "Profile saved but username sync failed: ${error.message}. Please try again.") }
                                 }
                             )
                         } else {
                             _uiState.update { it.copy(isSaving = false, hasChanges = false) }
                             _navigationEvents.emit(EditProfileNavigation.NavigateBack)
                         }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${error.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Unexpected error: ${e.message}") }
            }
        }
    }
}
