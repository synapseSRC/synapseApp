package com.synapse.social.studioasinc.feature.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseFollowService
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

data class FollowButtonUiState(
    val isFollowing: Boolean = false,
    val isFollowRequested: Boolean = false,
    val isPrivate: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class FollowButtonViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val followService: SupabaseFollowService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowButtonUiState())
    val uiState: StateFlow<FollowButtonUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var targetUserId: String? = null
    private val isOperationInProgress = AtomicBoolean(false)

    fun initialize(targetUserId: String) {
        if (this.targetUserId != targetUserId) {
            _uiState.value = FollowButtonUiState(isLoading = true)
        }
        this.targetUserId = targetUserId

        viewModelScope.launch {
            currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null && currentUserId != targetUserId) {
                checkFollowStatus()
            } else {
                val privateResult = followService.isPrivateProfile(targetUserId)
                if (this@FollowButtonViewModel.targetUserId == targetUserId) {
                    _uiState.value = _uiState.value.copy(
                        isPrivate = privateResult.getOrDefault(true),
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun checkFollowStatus() {
        val currentUid = currentUserId ?: return
        val targetUid = targetUserId ?: return

        _uiState.value = _uiState.value.copy(isLoading = true)

        val privateResult = followService.isPrivateProfile(targetUid)
        val statusResult = followService.getFollowStatus(targetUid)
        if (this.targetUserId != targetUid) return
        if (statusResult.isSuccess) {
            val status = statusResult.getOrThrow()
            _uiState.value = _uiState.value.copy(
                isFollowing = status == ProfileFollowStatus.FOLLOWING,
                isFollowRequested = status == ProfileFollowStatus.REQUESTED,
                isPrivate = privateResult.getOrDefault(true),
                isLoading = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isFollowing = false,
                isFollowRequested = false,
                isPrivate = privateResult.getOrDefault(true),
                isLoading = false
            )
        }
    }

    fun toggleFollow() {
        val currentUid = currentUserId ?: return
        val targetUid = targetUserId ?: return

        if (!isOperationInProgress.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                _uiState.value = currentState.copy(isLoading = true)

                val result = if (currentState.isFollowing || currentState.isFollowRequested) {
                    followService.unfollowUser(currentUid, targetUid)
                } else {
                    followService.followUser(currentUid, targetUid)
                }

                result.fold(
                    onSuccess = { checkFollowStatus() },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                )
            } finally {
                isOperationInProgress.set(false)
            }
        }
    }
}
