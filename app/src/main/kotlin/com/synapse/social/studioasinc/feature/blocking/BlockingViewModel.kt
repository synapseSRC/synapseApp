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
    
    /**
     * Blocks a user by their ID.
     * Updates UI state to Loading, then to BlockSuccess or Error based on result.
     * 
     * @param userId The ID of the user to block
     */
    fun blockUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = BlockingUiState.Loading
            
            blockUserUseCase(userId)
                .onSuccess {
                    _uiState.value = BlockingUiState.BlockSuccess
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
            
            unblockUserUseCase(userId)
                .onSuccess {
                    _uiState.value = BlockingUiState.UnblockSuccess
                    // Refresh the list after successful unblock
                    loadBlockedUsers()
                }
                .onFailure { error ->
                    _uiState.value = BlockingUiState.Error(
                        message = error.message ?: "Failed to unblock user"
                    )
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
     * Checks if a user is blocked.
     * Does not update UI state - provides result via callback.
     * 
     * @param userId The ID of the user to check
     * @param onResult Callback invoked with the result (true if blocked, false otherwise)
     */
    fun checkIfBlocked(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isUserBlockedUseCase(userId)
                .onSuccess { isBlocked ->
                    onResult(isBlocked)
                }
                .onFailure {
                    // On error, assume not blocked
                    onResult(false)
                }
        }
    }
    
    /**
     * Resets UI state to idle.
     * Useful for clearing success or error states after they've been handled.
     */
    fun resetState() {
        _uiState.value = BlockingUiState.Idle
    }
}
