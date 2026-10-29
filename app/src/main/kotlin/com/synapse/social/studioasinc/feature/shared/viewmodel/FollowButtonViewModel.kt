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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
    private var statusCheckSequence = 0L

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

        // Increment sequence to track this check - discard stale results from older calls
        val currentSequence = ++statusCheckSequence

        val privateResult = followService.isPrivateProfile(targetUid)
        val statusResult = followService.getFollowStatus(targetUid)
        
        // Discard if a newer check has started or target changed
        if (this.targetUserId != targetUid || statusCheckSequence != currentSequence) return

        val previous = _uiState.value
        if (statusResult.isSuccess) {
            val status = statusResult.getOrThrow()
            _uiState.value = previous.copy(
                isFollowing = status == ProfileFollowStatus.FOLLOWING,
                isFollowRequested = status == ProfileFollowStatus.REQUESTED,
                isPrivate = privateResult.getOrDefault(true),
                isLoading = false
            )
        } else {
            // Preserve last-known state on read failure instead of defaulting to "not following".
            _uiState.value = previous.copy(
                isPrivate = privateResult.getOrDefault(previous.isPrivate),
                isLoading = false
            )
        }
    }

    fun toggleFollow() {
        val currentUid = currentUserId ?: return
        val targetUid = targetUserId ?: return

        if (!isOperationInProgress.compareAndSet(false, true)) return

        val previousState = _uiState.value
        val wasFollowingOrRequested = previousState.isFollowing || previousState.isFollowRequested

        // Optimistic update: flip state immediately so the button changes instantly
        _uiState.value = if (wasFollowingOrRequested) {
            previousState.copy(isFollowing = false, isFollowRequested = false, isLoading = false)
        } else {
            previousState.copy(
                isFollowing = !previousState.isPrivate,
                isFollowRequested = previousState.isPrivate,
                isLoading = false
            )
        }

        viewModelScope.launch {
            try {
                val result = if (wasFollowingOrRequested) {
                    followService.unfollowUser(currentUid, targetUid)
                } else {
                    followService.followUser(currentUid, targetUid)
                }

                result.fold(
                    onSuccess = {
                        // Confirm with server state in background (non-blocking)
                        checkFollowStatus()
                    },
                    onFailure = { error ->
                        // Revert optimistic update on failure and show error
                        android.util.Log.e("FollowButtonViewModel", "Follow operation failed: ${error.message}", error)
                        _uiState.value = previousState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to follow user"
                        )
                    }
                )
            } finally {
                isOperationInProgress.set(false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
