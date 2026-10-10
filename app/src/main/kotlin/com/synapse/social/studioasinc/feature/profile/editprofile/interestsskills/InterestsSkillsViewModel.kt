package com.synapse.social.studioasinc.feature.profile.editprofile.interestsskills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.usecase.profile.GetInterestsSkillsUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.UpdateInterestsUseCase
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
class InterestsSkillsViewModel @Inject constructor(
    private val getInterestsSkillsUseCase: GetInterestsSkillsUseCase,
    private val updateInterestsUseCase: UpdateInterestsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestsSkillsUiState())
    val uiState: StateFlow<InterestsSkillsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<InterestsSkillsNavigation>()
    val navigationEvents: SharedFlow<InterestsSkillsNavigation> = _navigationEvents.asSharedFlow()

    private var initialInterests: List<String> = emptyList()

    init {
        loadProfile()
    }

    fun onEvent(event: InterestsSkillsEvent) {
        when (event) {
            InterestsSkillsEvent.LoadProfile -> loadProfile()
            is InterestsSkillsEvent.InterestInputChanged -> {
                _uiState.update { it.copy(pendingInterestInput = event.text, inputError = null) }
            }
            InterestsSkillsEvent.AddInterest -> addInterest()
            is InterestsSkillsEvent.RemoveInterest -> removeInterest(event.index)
            is InterestsSkillsEvent.MoveInterest -> moveInterest(event.fromIndex, event.toIndex)
            InterestsSkillsEvent.SaveClicked -> saveInterests()
            InterestsSkillsEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(InterestsSkillsNavigation.NavigateBack) }
            }
            InterestsSkillsEvent.DismissError -> _uiState.update { it.copy(error = null) }
            InterestsSkillsEvent.DismissSuccess -> _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = getInterestsSkillsUseCase.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            getInterestsSkillsUseCase(userId).collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        initialInterests = profile.interests
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                interests = profile.interests,
                                hasChanges = false,
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

    private fun addInterest() {
        val rawInput = _uiState.value.pendingInterestInput
        val trimmedInput = rawInput.trim()

        if (trimmedInput.isBlank()) {
            _uiState.update { it.copy(inputError = "Interest cannot be empty") }
            return
        }

        val currentInterests = _uiState.value.interests
        if (currentInterests.any { it.equals(trimmedInput, ignoreCase = true) }) {
            _uiState.update { it.copy(inputError = "Interest already exists") }
            return
        }

        val updatedList = currentInterests + trimmedInput
        _uiState.update { currentState ->
            currentState.copy(
                interests = updatedList,
                pendingInterestInput = "",
                inputError = null,
                hasChanges = updatedList != initialInterests
            )
        }
    }

    private fun removeInterest(index: Int) {
        val currentInterests = _uiState.value.interests
        if (index !in currentInterests.indices) return

        val updatedList = currentInterests.toMutableList().apply { removeAt(index) }
        _uiState.update { currentState ->
            currentState.copy(
                interests = updatedList,
                hasChanges = updatedList != initialInterests
            )
        }
    }

    private fun moveInterest(fromIndex: Int, toIndex: Int) {
        val currentInterests = _uiState.value.interests
        if (fromIndex !in currentInterests.indices || toIndex !in currentInterests.indices) return
        if (fromIndex == toIndex) return

        val updatedList = currentInterests.toMutableList()
        val item = updatedList.removeAt(fromIndex)
        updatedList.add(toIndex, item)

        _uiState.update { currentState ->
            currentState.copy(
                interests = updatedList,
                hasChanges = updatedList != initialInterests
            )
        }
    }

    private fun saveInterests() {
        if (!_uiState.value.hasChanges) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            val userId = getInterestsSkillsUseCase.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            val result = updateInterestsUseCase(userId, _uiState.value.interests)
            result.fold(
                onSuccess = {
                    initialInterests = _uiState.value.interests
                    _uiState.update { currentState ->
                        currentState.copy(
                            isSaving = false,
                            hasChanges = false,
                            successMessage = "Interests saved successfully"
                        )
                    }
                    _navigationEvents.emit(InterestsSkillsNavigation.NavigateBack)
                },
                onFailure = { error ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save interests"
                        )
                    }
                }
            )
        }
    }
}
