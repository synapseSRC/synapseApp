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
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    fun loadUsers(userId: String, listType: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result: Result<List<SharedUser>> = when (listType) {
                    "followers" -> getFollowersUseCase(userId)
                    "following" -> getFollowingUseCase(userId)
                    else -> Result.success(emptyList())
                }

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
                            users = users,
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load users: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}
