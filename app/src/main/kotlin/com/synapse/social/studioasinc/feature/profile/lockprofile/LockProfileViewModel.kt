package com.synapse.social.studioasinc.feature.profile.lockprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.LockProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockProfileUiState(
    val isPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LockProfileViewModel @Inject constructor(
    private val lockProfileUseCase: LockProfileUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockProfileUiState())
    val uiState: StateFlow<LockProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = getCurrentUserIdUseCase()
            currentUserId = userId

            if (userId != null) {
                getProfileUseCase(userId).collect { result ->
                    result.onSuccess { profile ->
                        _uiState.update { it.copy(isPrivate = profile.isPrivate, isLoading = false) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(error = error.message, isLoading = false) }
                    }
                }
            } else {
                _uiState.update { it.copy(error = "User not logged in", isLoading = false) }
            }
        }
    }

    fun toggleLock(isLocked: Boolean) {
        _uiState.update { it.copy(isPrivate = isLocked) }
    }

    fun save() {
        val userId = currentUserId ?: return
        val isLocked = _uiState.value.isPrivate

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            lockProfileUseCase(userId, isLocked).collect { result ->
                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}
