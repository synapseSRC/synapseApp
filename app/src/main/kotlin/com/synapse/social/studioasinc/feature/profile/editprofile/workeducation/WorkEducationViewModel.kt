package com.synapse.social.studioasinc.feature.profile.editprofile.workeducation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.shared.core.util.EducationSanitizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkEducationViewModel @Inject constructor(
    private val repository: EditProfileRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkEducationUiState())
    val uiState: StateFlow<WorkEducationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<WorkEducationNavigation>()
    val navigationEvents: SharedFlow<WorkEducationNavigation> = _navigationEvents.asSharedFlow()

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
                        val occ = profile.occupation ?: ""
                        val wp = profile.workplace ?: ""
                        val edu = EducationSanitizer.sanitizeEducationString(profile.educationDisplay) ?: ""

                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                profile = profile,
                                occupation = occ,
                                workplace = wp,
                                education = edu,
                                initialOccupation = occ,
                                initialWorkplace = wp,
                                initialEducation = edu,
                                error = null
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

    fun onEvent(event: WorkEducationEvent) {
        when (event) {
            is WorkEducationEvent.OccupationChanged -> {
                _uiState.update { it.copy(occupation = event.occupation) }
            }
            is WorkEducationEvent.WorkplaceChanged -> {
                _uiState.update { it.copy(workplace = event.workplace) }
            }
            is WorkEducationEvent.EducationChanged -> {
                _uiState.update { it.copy(education = event.education) }
            }
            WorkEducationEvent.SaveClicked -> {
                saveProfile()
            }
            WorkEducationEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(WorkEducationNavigation.NavigateBack) }
            }
            WorkEducationEvent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        if (!state.hasChanges || state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = repository.getCurrentUserId()

            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            val updateData = mutableMapOf<String, Any?>()

            if (state.occupation.isNotBlank()) {
                updateData["occupation"] = state.occupation.trim()
            } else {
                updateData["occupation"] = null
            }

            if (state.workplace.isNotBlank()) {
                updateData["workplace"] = state.workplace.trim()
            } else {
                updateData["workplace"] = null
            }

            val rawEducationList = state.education.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val educationList = EducationSanitizer.sanitizeEducationList(rawEducationList)
            if (educationList.isNotEmpty()) {
                updateData["education"] = educationList
            } else {
                updateData["education"] = emptyList<String>()
            }

            val result = repository.updateProfile(userId, updateData)

            result.fold(
                onSuccess = {
                    val occ = if (state.occupation.isNotBlank()) state.occupation.trim() else ""
                    val wp = if (state.workplace.isNotBlank()) state.workplace.trim() else ""
                    val edu = EducationSanitizer.sanitizeEducationString(educationList.joinToString(", ")) ?: ""

                    _uiState.update { currentState ->
                        currentState.copy(
                            isSaving = false,
                            occupation = occ,
                            workplace = wp,
                            education = edu,
                            initialOccupation = occ,
                            initialWorkplace = wp,
                            initialEducation = edu
                        )
                    }
                    _navigationEvents.emit(WorkEducationNavigation.NavigateBack)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "Failed to save profile") }
                }
            )
        }
    }
}