package com.synapse.social.studioasinc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.shared.domain.model.User as SharedUser

data class UserListState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FollowListUiState(
    val followers: UserListState = UserListState(),
    val following: UserListState = UserListState()
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    fun loadUsers(userId: String) {
        _uiState.update {
            it.copy(
                followers = it.followers.copy(isLoading = true, error = null),
                following = it.following.copy(isLoading = true, error = null)
            )
        }

        viewModelScope.launch {
            try {
                val result = getFollowersUseCase(userId)
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
                        _uiState.update {
                            it.copy(followers = it.followers.copy(users = users, isLoading = false))
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(followers = it.followers.copy(isLoading = false, error = "Failed to load followers: ${error.message}"))
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(followers = it.followers.copy(isLoading = false, error = "Error: ${e.message}"))
                }
            }
        }

        viewModelScope.launch {
            try {
                val result = getFollowingUseCase(userId)
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
                        _uiState.update {
                            it.copy(following = it.following.copy(users = users, isLoading = false))
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(following = it.following.copy(isLoading = false, error = "Failed to load following: ${error.message}"))
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(following = it.following.copy(isLoading = false, error = "Error: ${e.message}"))
                }
            }
        }
    }
}
