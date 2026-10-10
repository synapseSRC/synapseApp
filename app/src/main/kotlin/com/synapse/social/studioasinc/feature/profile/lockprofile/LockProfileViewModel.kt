package com.synapse.social.studioasinc.feature.profile.lockprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.usecase.profile.GetProfileUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.GetPendingFollowRequestsUseCase
import com.synapse.social.studioasinc.domain.usecase.profile.RespondToFollowRequestUseCase
import com.synapse.social.studioasinc.domain.model.ProfileFollowRequest
import com.synapse.social.studioasinc.domain.usecase.profile.LockProfileUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockProfileUiState(
    val isPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val isProfileLoaded: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val followRequests: List<ProfileFollowRequest> = emptyList(),
    val isLoadingRequests: Boolean = false,
    val requestsError: String? = null,
    val respondingToRequestId: String? = null
)

@HiltViewModel
class LockProfileViewModel @Inject constructor(
    private val lockProfileUseCase: LockProfileUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getPendingFollowRequestsUseCase: GetPendingFollowRequestsUseCase,
    private val respondToFollowRequestUseCase: RespondToFollowRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockProfileUiState())
    val uiState: StateFlow<LockProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var persistedIsPrivate: Boolean = false

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = getCurrentUserIdUseCase()
            currentUserId = userId

            if (userId != null) {
                getProfileUseCase(userId).take(1).collect { result ->
                    result.onSuccess { profile ->
                        persistedIsPrivate = profile.isPrivate
                        _uiState.update { it.copy(isPrivate = profile.isPrivate, isLoading = false, isProfileLoaded = true, error = null) }
                        if (profile.isPrivate) loadPendingRequests()
                    }.onFailure { error ->
                        _uiState.update { it.copy(error = error.message, isLoading = false) }
                    }
                }
            } else {
                _uiState.update { it.copy(error = "User not logged in", isLoading = false) }
            }
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRequests = true, requestsError = null) }
            getPendingFollowRequestsUseCase().onSuccess { requests ->
                _uiState.update { it.copy(followRequests = requests, isLoadingRequests = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoadingRequests = false, requestsError = error.message) }
            }
        }
    }

    fun respondToRequest(requesterUid: String, accept: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(respondingToRequestId = requesterUid, requestsError = null) }
            respondToFollowRequestUseCase(requesterUid, accept).onSuccess {
                _uiState.update { state ->
                    state.copy(
                        followRequests = state.followRequests.filterNot { it.requesterId == requesterUid },
                        respondingToRequestId = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(respondingToRequestId = null, requestsError = error.message) }
            }
        }
    }

    fun toggleLock(isLocked: Boolean) {
        if (!_uiState.value.isProfileLoaded || _uiState.value.isLoading) return
        _uiState.update { it.copy(isPrivate = isLocked, error = null) }
    }

    fun save() {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(error = "User not logged in") }
            return
        }
        val isLocked = _uiState.value.isPrivate

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            lockProfileUseCase(userId, isLocked).collect { result ->
                result.onSuccess {
                    persistedIsPrivate = isLocked
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isPrivate = persistedIsPrivate, isLoading = false, error = error.message) }
                }
            }
        }
    }
}
