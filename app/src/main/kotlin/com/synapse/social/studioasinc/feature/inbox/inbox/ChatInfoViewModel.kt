package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.ChatLockManager
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.IsUserBlockedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.UnblockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteConversationUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetSharedContentSummaryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatInfoUiState(
    val isLoading: Boolean = true,
    val userProfile: User? = null,
    val isOnline: Boolean = false,
    val isBlocked: Boolean = false,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val disappearingMode: DisappearingMode = DisappearingMode.OFF,
    val mediaCount: Int = 0,
    val linkCount: Int = 0,
    val fileCount: Int = 0,
    val isSharedContentSample: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    @StringRes val userMessageRes: Int? = null
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase,
    private val getDisappearingModeUseCase: GetDisappearingModeUseCase,
    private val setDisappearingModeUseCase: SetDisappearingModeUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val isUserBlockedUseCase: IsUserBlockedUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val getSharedContentSummaryUseCase: GetSharedContentSummaryUseCase,
    private val chatLockManager: ChatLockManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatInfoUiState())
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    private var presenceJob: Job? = null
    private var initializedChatId: String? = null
    private var initializedUserId: String? = null

    fun loadChatInfo(chatId: String, userId: String) {
        if (initializedChatId == chatId && initializedUserId == userId) return
        initializedChatId = chatId
        initializedUserId = userId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }

            // Load user profile if userId is valid
            if (userId.isNotBlank()) {
                getUserProfileUseCase(userId)
                    .onSuccess { user ->
                        _uiState.update { it.copy(userProfile = user) }
                    }
                    .onFailure {
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_profile) }
                    }

                // Check block status
                isUserBlockedUseCase(userId)
                    .onSuccess { blocked ->
                        _uiState.update { it.copy(isBlocked = blocked) }
                    }

                // Observe presence status
                presenceJob?.cancel()
                presenceJob = viewModelScope.launch {
                    observeUserPresenceUseCase(userId).collect { active ->
                        _uiState.update { it.copy(isOnline = active) }
                    }
                }
            }

            // Check chat lock and settings
            if (chatId.isNotBlank()) {
                val locked = chatLockManager.isChatLocked(chatId)
                _uiState.update { it.copy(isLocked = locked) }

                // Fetch disappearing mode
                getDisappearingModeUseCase(chatId)
                    .onSuccess { mode ->
                        _uiState.update { it.copy(disappearingMode = mode) }
                    }

                // Fetch shared content summary
                getSharedContentSummaryUseCase(chatId)
                    .onSuccess { summary ->
                        _uiState.update {
                            it.copy(
                                mediaCount = summary.mediaCount,
                                linkCount = summary.linkCount,
                                fileCount = summary.fileCount,
                                isSharedContentSample = summary.isSample
                            )
                        }
                    }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun setDisappearingMode(chatId: String, mode: DisappearingMode) {
        viewModelScope.launch {
            setDisappearingModeUseCase(chatId, mode)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            disappearingMode = mode,
                            userMessageRes = R.string.toast_disappearing_mode_updated
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_update_disappearing_mode) }
                }
        }
    }

    fun toggleChatLock(chatId: String) {
        val currentLocked = _uiState.value.isLocked
        if (currentLocked) {
            chatLockManager.unlockChat(chatId)
            _uiState.update { it.copy(isLocked = false) }
        } else {
            chatLockManager.lockChat(chatId)
            _uiState.update { it.copy(isLocked = true) }
        }
    }

    fun toggleMuteNotifications() {
        val currentlyMuted = _uiState.value.isMuted
        _uiState.update {
            it.copy(
                isMuted = !currentlyMuted,
                userMessageRes = if (!currentlyMuted) R.string.muted else R.string.unmuted
            )
        }
    }

    fun toggleBlockUser(userId: String) {
        if (userId.isBlank()) return
        val currentlyBlocked = _uiState.value.isBlocked
        viewModelScope.launch {
            if (currentlyBlocked) {
                unblockUserUseCase(userId)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isBlocked = false,
                                userMessageRes = R.string.chat_info_msg_unblocked
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_unblock) }
                    }
            } else {
                blockUserUseCase(userId)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isBlocked = true,
                                userMessageRes = R.string.chat_info_msg_blocked
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_block) }
                    }
            }
        }
    }

    fun clearChat(chatId: String, onCleared: () -> Unit) {
        if (chatId.isBlank()) return
        viewModelScope.launch {
            deleteConversationUseCase(chatId)
                .onSuccess {
                    _uiState.update { it.copy(userMessageRes = R.string.chat_info_msg_cleared) }
                    onCleared()
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_clear_chat) }
                }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessageRes = null) }
    }
}
