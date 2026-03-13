package com.synapse.social.studioasinc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.shared.domain.model.User as SharedUser

data class FollowListUiState(
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val followersLoading: Boolean = false,
    val followingLoading: Boolean = false,
    val followersError: String? = null,
    val followingError: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(followersLoading = true, followersError = null)

            try {
                val result: Result<List<SharedUser>> = getFollowersUseCase(userId)

                result.fold(
                    onSuccess = { sharedUsers ->
                        val users = sharedUsers.map { sharedUser ->
                            User(
                                uid = sharedUser.uid,
                                username = sharedUser.username,
                                displayName = sharedUser.displayName,
                                avatar = sharedUser.avatar,
                                verify = sharedUser.verify
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            followers = users,
                            followersLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            followersLoading = false,
                            followersError = "Failed to load followers: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    followersLoading = false,
                    followersError = "Error: ${e.message}"
                )
            }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(followingLoading = true, followingError = null)

            try {
                val result: Result<List<SharedUser>> = getFollowingUseCase(userId)

                result.fold(
                    onSuccess = { sharedUsers ->
                        val users = sharedUsers.map { sharedUser ->
                            User(
                                uid = sharedUser.uid,
                                username = sharedUser.username,
                                displayName = sharedUser.displayName,
                                avatar = sharedUser.avatar,
                                verify = sharedUser.verify
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            following = users,
                            followingLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            followingLoading = false,
                            followingError = "Failed to load following: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    followingLoading = false,
                    followingError = "Error: ${e.message}"
                )
            }
        }
    }
}
