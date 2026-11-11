package com.synapse.social.studioasinc.feature.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.GetBlockedUsersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.IsUserBlockedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.UnblockUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for blocking feature.
 * Manages UI state using StateFlow and delegates business logic to use cases.
 * 
 * @param blockUserUseCase Use case for blocking a user
 * @param unblockUserUseCase Use case for unblocking a user
 * @param getBlockedUsersUseCase Use case for retrieving blocked users
 * @param isUserBlockedUseCase Use case for checking if a user is blocked
 */
@HiltViewModel
class BlockingViewModel @Inject constructor(
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val isUserBlockedUseCase: IsUserBlockedUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BlockingUiState>(BlockingUiState.Idle)
    val uiState: StateFlow<BlockingUiState> = _uiState.asStateFlow()
    
    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _refreshFailed = MutableStateFlow(false)
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()
    
    /**
     * Blocks a user by their ID.
     * Updates UI state to Loading, then to BlockSuccess or Error based on result.
     * On success, refreshes the blocked users list inline to avoid a state-race
     * with loadBlockedUsers() (which resets state to Loading unconditionally).
     * @param userId The ID of the user to block
     */
    fun blockUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = BlockingUiState.Loading
            
            blockUserUseCase(userId)
                .onSuccess {
                    _uiState.value = BlockingUiState.BlockSuccess
                    // Refresh the blocked users list directly without going through
                    // loadBlockedUsers(), which would overwrite BlockSuccess with Loading
                    // before the UI LaunchedEffect can observe it (state-race fix).
                    getBlockedUsersUseCase()
                        .onSuccess { users ->
                            _blockedUsers.value = users
                        }
                    // Transition to Idle; any list-load failure is non-fatal here.
                    _uiState.value = BlockingUiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = BlockingUiState.Error(
                        message = error.message ?: "Failed to block user"
                    )
                }
        }
    }
    
    /**
     * Unblocks a user by their ID.
     * Updates UI state to Loading, then to UnblockSuccess or Error based on result.
     * Automatically refreshes the blocked users list on success.
     * 
     * @param userId The ID of the user to unblock
     */
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = BlockingUiState.Loading
            
            val result = unblockUserUseCase(userId)
            result.onSuccess {
                _uiState.value = BlockingUiState.UnblockSuccess
            }.onFailure { error ->
                _uiState.value = BlockingUiState.Error(
                    message = error.message ?: "Failed to unblock user"
                )
            }

            if (result.isSuccess) {
                // Refresh the data without replacing UnblockSuccess with Loading/Idle.
                refreshBlockedUsers()
            }
        }
    }
    
    /**
     * Loads the list of blocked users.
     * Updates UI state to Loading, then to Idle or Error based on result.
     * Updates the blockedUsers StateFlow with the retrieved list.
     */
    fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.value = BlockingUiState.Loading
            
            getBlockedUsersUseCase()
                .onSuccess { users ->
                    _blockedUsers.value = users
                    _uiState.value = BlockingUiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = BlockingUiState.Error(
                        message = error.message ?: "Failed to load blocked users"
                    )
                }
        }
    }
    
    
    /**
     * Refreshes blocked-user data without touching the action UI state.
     * This keeps success events observable while the network refresh is in flight.
     */
    private suspend fun refreshBlockedUsers() {
        _refreshFailed.value = false
        getBlockedUsersUseCase()
            .onSuccess { users ->
                _blockedUsers.value = users
            }
            .onFailure {
                _refreshFailed.value = true
            }
    }

    fun clearRefreshError() {
        _refreshFailed.value = false
    }

    /**
     * Resets UI state to idle.
     * Useful for clearing success or error states after they've been handled.
     */
    fun resetState() {
        _uiState.value = BlockingUiState.Idle
    }
}
