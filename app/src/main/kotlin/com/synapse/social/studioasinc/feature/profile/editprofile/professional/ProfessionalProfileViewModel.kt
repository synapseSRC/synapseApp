package com.synapse.social.studioasinc.feature.profile.editprofile.professional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.BusinessRepository
import com.synapse.social.studioasinc.shared.domain.model.business.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfessionalProfileViewModel @Inject constructor(
    private val businessRepository: BusinessRepository,
    private val editProfileRepository: EditProfileRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfessionalProfileUiState())
    val uiState: StateFlow<ProfessionalProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, switchAccountError = null) }

            val userId = editProfileRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            try {
                val businessAccountResult = businessRepository.getBusinessAccount(userId)
                val businessAccount = businessAccountResult.getOrNull()
                val accountType = businessAccount?.accountType ?: AccountType.PERSONAL

                val profileResult = runCatching {
                    editProfileRepository.getUserProfile(userId).first()
                }

                profileResult.fold(
                    onSuccess = { result ->
                        result.fold(
                            onSuccess = { profile ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        accountType = accountType,
                                        isBusinessAccount = accountType != AccountType.PERSONAL,
                                        occupation = profile.occupation ?: "",
                                        workplace = profile.workplace ?: ""
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.update { it.copy(isLoading = false, error = error.message) }
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onOccupationChange(occupation: String) {
        _uiState.update { it.copy(occupation = occupation, hasChanges = true) }
    }

    fun onWorkplaceChange(workplace: String) {
        _uiState.update { it.copy(workplace = workplace, hasChanges = true) }
    }

    fun switchToBusinessAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, switchAccountError = null) }
            val userId = editProfileRepository.getCurrentUserId() ?: return@launch

            businessRepository.createBusinessAccount(userId)
                .onSuccess {
                    loadData()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, switchAccountError = e.message) }
                }
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = editProfileRepository.getCurrentUserId() ?: return@launch

            try {
                val updateData = mapOf(
                    "occupation" to state.occupation.ifBlank { null },
                    "workplace" to state.workplace.ifBlank { null }
                )

                val result = editProfileRepository.updateProfile(userId, updateData)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSaving = false, hasChanges = false) }
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

    fun clearError() {
        _uiState.update { it.copy(error = null, switchAccountError = null) }
    }
}
