package com.synapse.social.studioasinc.feature.profile.editprofile.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
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
class LocationViewModel @Inject constructor(
    private val repository: EditProfileRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<LocationNavigation>()
    val navigationEvents: SharedFlow<LocationNavigation> = _navigationEvents.asSharedFlow()

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
                        val reg = profile.region ?: ""
                        val city = profile.currentCity ?: ""
                        val home = profile.hometown ?: ""

                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                profile = profile,
                                selectedRegion = reg,
                                currentCity = city,
                                hometown = home,
                                initialSelectedRegion = reg,
                                initialCurrentCity = city,
                                initialHometown = home,
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

    fun onEvent(event: LocationEvent) {
        when (event) {
            is LocationEvent.RegionSelected -> {
                _uiState.update { it.copy(selectedRegion = event.region) }
            }
            is LocationEvent.CurrentCityChanged -> {
                _uiState.update { it.copy(currentCity = event.city) }
            }
            is LocationEvent.HometownChanged -> {
                _uiState.update { it.copy(hometown = event.hometown) }
            }
            LocationEvent.SelectRegionClicked -> {
                viewModelScope.launch {
                    _navigationEvents.emit(
                        LocationNavigation.NavigateToRegionSelection(_uiState.value.selectedRegion)
                    )
                }
            }
            LocationEvent.SaveClicked -> {
                saveProfile()
            }
            LocationEvent.BackClicked -> {
                viewModelScope.launch { _navigationEvents.emit(LocationNavigation.NavigateBack) }
            }
            LocationEvent.DismissError -> {
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

            val reg = state.selectedRegion.trim()
            if (reg.isNotBlank()) {
                updateData["region"] = reg
            } else {
                updateData["region"] = null
            }

            val city = state.currentCity.trim()
            if (city.isNotBlank()) {
                updateData["current_city"] = city
            } else {
                updateData["current_city"] = null
            }

            val home = state.hometown.trim()
            if (home.isNotBlank()) {
                updateData["hometown"] = home
            } else {
                updateData["hometown"] = null
            }

            val result = repository.updateProfile(userId, updateData)

            result.fold(
                onSuccess = {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isSaving = false,
                            selectedRegion = reg,
                            currentCity = city,
                            hometown = home,
                            initialSelectedRegion = reg,
                            initialCurrentCity = city,
                            initialHometown = home
                        )
                    }
                    _navigationEvents.emit(LocationNavigation.NavigateBack)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "Failed to save profile") }
                }
            )
        }
    }
}
