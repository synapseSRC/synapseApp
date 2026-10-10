package com.synapse.social.studioasinc.feature.profile.personalinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.model.RelationshipStatus
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.domain.model.Gender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PersonalInformationViewModel @Inject constructor(
    private val repository: EditProfileRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalInformationUiState())
    val uiState: StateFlow<PersonalInformationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<PersonalInformationNavigation>()
    val navigationEvents: SharedFlow<PersonalInformationNavigation> = _navigationEvents.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
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
                        val relStatus = RelationshipStatus.fromDisplayName(profile.relationshipStatus)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                pronouns = profile.pronouns ?: "",
                                birthday = profile.birthday ?: "",
                                gender = profile.safeGender,
                                relationshipStatus = relStatus,
                                hasChanges = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load profile") }
                    }
                )
            }
        }
    }

    fun onEvent(event: PersonalInformationEvent) {
        when (event) {
            is PersonalInformationEvent.PronounsChanged -> {
                _uiState.update { it.copy(pronouns = event.pronouns, hasChanges = true) }
            }
            is PersonalInformationEvent.BirthdayChanged -> {
                _uiState.update { it.copy(birthday = event.birthday, hasChanges = true) }
            }
            is PersonalInformationEvent.GenderSelected -> {
                _uiState.update { it.copy(gender = event.gender, hasChanges = true) }
            }
            is PersonalInformationEvent.RelationshipStatusSelected -> {
                _uiState.update { it.copy(relationshipStatus = event.status, hasChanges = true) }
            }
            PersonalInformationEvent.SaveClicked -> {
                saveProfile()
            }
            PersonalInformationEvent.BackClicked -> {
                viewModelScope.launch {
                    _navigationEvents.emit(PersonalInformationNavigation.NavigateBack)
                }
            }
            PersonalInformationEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            val updateData = mutableMapOf<String, Any?>()

            if (state.pronouns.isNotBlank()) {
                updateData["pronouns"] = state.pronouns
            } else {
                updateData["pronouns"] = null
            }

            if (state.birthday.isNotBlank()) {
                updateData["birthday"] = state.birthday
            } else {
                updateData["birthday"] = null
            }

            updateData["gender"] = state.gender.name.lowercase()

            if (state.relationshipStatus != null) {
                updateData["relationship_status"] = state.relationshipStatus.displayName
            } else {
                updateData["relationship_status"] = null
            }

            val result = repository.updateProfile(userId, updateData)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, hasChanges = false, saveSuccess = true) }
                    _navigationEvents.emit(PersonalInformationNavigation.NavigateBack)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "Failed to save personal information") }
                }
            )
        }
    }

    companion object {
        fun formatLocaleDate(rawDateString: String): String {
            if (rawDateString.isBlank()) return ""
            return try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date: Date? = isoFormat.parse(rawDateString)
                if (date != null) {
                    val localeFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                    localeFormatter.format(date)
                } else {
                    rawDateString
                }
            } catch (e: Exception) {
                rawDateString
            }
        }
    }
}
