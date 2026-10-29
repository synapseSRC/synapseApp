package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.ChatLockManager
import com.synapse.social.studioasinc.domain.usecase.profile.ReportUserUseCase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.IsUserBlockedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.UnblockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ClearChatHistoryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetCommonGroupsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationMuteStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetSharedContentSummaryUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetConversationMuteStatusUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SetDisappearingModeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.ObserveUserPresenceDetailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.GetUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    val isMuteUpdating: Boolean = false,
    val disappearingMode: DisappearingMode = DisappearingMode.OFF,
    val mediaCount: Int = 0,
    val linkCount: Int = 0,
    val fileCount: Int = 0,
    val isSharedContentSample: Boolean = false,
    val isLoadingSharedContent: Boolean = false,
    @StringRes val sharedContentErrorRes: Int? = null,
    val commonGroups: List<Conversation> = emptyList(),
    val isLoadingCommonGroups: Boolean = false,
    @StringRes val commonGroupsErrorRes: Int? = null,
    @StringRes val errorMessageRes: Int? = null,
    val canRetryLoad: Boolean = false,
    @StringRes val userMessageRes: Int? = null
)

@HiltViewModel
class ChatInfoViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val observeUserPresenceUseCase: ObserveUserPresenceUseCase,
    private val observeUserPresenceDetailUseCase: ObserveUserPresenceDetailUseCase,
    private val getDisappearingModeUseCase: GetDisappearingModeUseCase,
    private val setDisappearingModeUseCase: SetDisappearingModeUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val isUserBlockedUseCase: IsUserBlockedUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val getSharedContentSummaryUseCase: GetSharedContentSummaryUseCase,
    private val getConversationMuteStatusUseCase: GetConversationMuteStatusUseCase,
    private val setConversationMuteStatusUseCase: SetConversationMuteStatusUseCase,
    private val getCommonGroupsUseCase: GetCommonGroupsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val reportUserUseCase: ReportUserUseCase,
    private val chatLockManager: ChatLockManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatInfoUiState())
    val uiState: StateFlow<ChatInfoUiState> = _uiState.asStateFlow()

    private var presenceJob: Job? = null
    private var initializedChatId: String? = null
    private var initializedUserId: String? = null

    fun retryLoadChatInfo(chatId: String, userId: String) {
        initializedChatId = null
        initializedUserId = null
        loadChatInfo(chatId, userId)
    }

    fun loadChatInfo(chatId: String, userId: String) {
        if (initializedChatId == chatId && initializedUserId == userId) return
        initializedChatId = chatId
        initializedUserId = userId
        presenceJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                userProfile = null,
                isOnline = false,
                isBlocked = false,
                isLocked = false,
                isMuted = false,
                isMuteUpdating = false,
                disappearingMode = DisappearingMode.OFF,
                mediaCount = 0,
                linkCount = 0,
                fileCount = 0,
                isSharedContentSample = false,
                isLoadingSharedContent = chatId.isNotBlank(),
                sharedContentErrorRes = null,
                commonGroups = emptyList(),
                isLoadingCommonGroups = userId.isNotBlank(),
                commonGroupsErrorRes = null,
                errorMessageRes = null,
                canRetryLoad = false,
                userMessageRes = null
            )
        }

        viewModelScope.launch {
            if (userId.isNotBlank()) {
                getUserProfileUseCase(userId)
                    .onSuccess { user -> _uiState.update { it.copy(userProfile = user) } }
                    .onFailure {
                        markLoadFailure()
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_profile) }
                    }

                isUserBlockedUseCase(userId)
                    .onSuccess { blocked -> _uiState.update { it.copy(isBlocked = blocked) } }
                    .onFailure {
                        markLoadFailure()
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_block_status) }
                    }

                presenceJob = viewModelScope.launch {
                    observeUserPresenceUseCase(userId).collect { active ->
                        _uiState.update { it.copy(isOnline = active) }
                    }
                }

                getCommonGroupsUseCase(userId)
                    .onSuccess { groupResult ->
                        _uiState.update {
                            it.copy(
                                commonGroups = groupResult.groups,
                                isLoadingCommonGroups = false,
                                commonGroupsErrorRes = if (groupResult.hasIncompleteResults) {
                                    R.string.chat_info_error_load_groups_partial
                                } else {
                                    null
                                }
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                isLoadingCommonGroups = false,
                                commonGroupsErrorRes = R.string.chat_info_error_load_groups
                            )
                        }
                    }
            }

                        if (chatId.isNotBlank()) {
                _uiState.update { it.copy(isLocked = chatLockManager.isChatLocked(chatId)) }

                getConversationMuteStatusUseCase(chatId)
                    .onSuccess { muted -> _uiState.update { it.copy(isMuted = muted) } }
                    .onFailure {
                        markLoadFailure()
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_mute) }
                    }

                getDisappearingModeUseCase(chatId)
                    .onSuccess { mode -> _uiState.update { it.copy(disappearingMode = mode) } }
                    .onFailure {
                        markLoadFailure()
                        _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_load_disappearing_mode) }
                    }

                getSharedContentSummaryUseCase(chatId)
                    .onSuccess { summary ->
                        _uiState.update {
                            it.copy(
                                mediaCount = summary.mediaCount,
                                linkCount = summary.linkCount,
                                fileCount = summary.fileCount,
                                isSharedContentSample = summary.isSample,
                                isLoadingSharedContent = false,
                                sharedContentErrorRes = null
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                isLoadingSharedContent = false,
                                sharedContentErrorRes = R.string.chat_info_error_load_shared_content
                            )
                        }
                    }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun retryCommonGroups(userId: String) {
        if (userId.isBlank() || _uiState.value.isLoadingCommonGroups) return
        _uiState.update { it.copy(isLoadingCommonGroups = true, commonGroupsErrorRes = null) }
        viewModelScope.launch {
            getCommonGroupsUseCase(userId)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            commonGroups = result.groups,
                            isLoadingCommonGroups = false,
                            commonGroupsErrorRes = if (result.hasIncompleteResults) {
                                R.string.chat_info_error_load_groups_partial
                            } else {
                                null
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoadingCommonGroups = false, commonGroupsErrorRes = R.string.chat_info_error_load_groups)
                    }
                }
        }
    }

    fun retrySharedContent(chatId: String) {
        if (chatId.isBlank() || _uiState.value.isLoadingSharedContent) return
        _uiState.update { it.copy(isLoadingSharedContent = true, sharedContentErrorRes = null) }
        viewModelScope.launch {
            getSharedContentSummaryUseCase(chatId)
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            mediaCount = summary.mediaCount,
                            linkCount = summary.linkCount,
                            fileCount = summary.fileCount,
                            isSharedContentSample = summary.isSample,
                            isLoadingSharedContent = false,
                            sharedContentErrorRes = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoadingSharedContent = false, sharedContentErrorRes = R.string.chat_info_error_load_shared_content)
                    }
                }
        }
    }

    private fun markLoadFailure() {
        initializedChatId = null
        initializedUserId = null
        _uiState.update { it.copy(canRetryLoad = true) }
    }

    fun setDisappearingMode(chatId: String, mode: DisappearingMode) {
        if (chatId.isBlank()) return
        viewModelScope.launch {
            setDisappearingModeUseCase(chatId, mode)
                .onSuccess {
                    _uiState.update {
                        it.copy(disappearingMode = mode, userMessageRes = R.string.toast_disappearing_mode_updated)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_update_disappearing_mode) }
                }
        }
    }

    fun toggleChatLock(chatId: String) {
        if (chatId.isBlank()) return
        val currentLocked = _uiState.value.isLocked
        if (currentLocked) {
            chatLockManager.unlockChat(chatId)
            _uiState.update { it.copy(isLocked = false) }
        } else {
            chatLockManager.lockChat(chatId)
            _uiState.update { it.copy(isLocked = true) }
        }
    }

    fun toggleMuteNotifications(chatId: String) {
        if (chatId.isBlank() || _uiState.value.isMuteUpdating) return
        val newMutedState = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuteUpdating = true) }
        viewModelScope.launch {
            setConversationMuteStatusUseCase(chatId, newMutedState)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isMuted = newMutedState,
                            isMuteUpdating = false,
                            userMessageRes = if (newMutedState) R.string.muted else R.string.unmuted
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isMuteUpdating = false, errorMessageRes = R.string.chat_info_error_update_mute)
                    }
                }
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
                            it.copy(isBlocked = false, userMessageRes = R.string.chat_info_msg_unblocked)
                        }
                    }
                    .onFailure { _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_unblock) } }
            } else {
                blockUserUseCase(userId)
                    .onSuccess {
                        _uiState.update {
                            it.copy(isBlocked = true, userMessageRes = R.string.chat_info_msg_blocked)
                        }
                    }
                    .onFailure { _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_block) } }
            }
        }
    }

    fun reportUser(reportedUserId: String, reason: String) {
        if (reportedUserId.isBlank() || reason.isBlank()) return
        val reporterId = getCurrentUserIdUseCase().orEmpty()
        if (reporterId.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_report_user) }
            return
        }
        viewModelScope.launch {
            reportUserUseCase(reporterId, reportedUserId, reason).collect { result ->
                result.onSuccess {
                    _uiState.update { it.copy(userMessageRes = R.string.chat_info_msg_reported) }
                }.onFailure {
                    _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_report_user) }
                }
            }
        }
    }

    fun clearChat(chatId: String, onCleared: () -> Unit) {
        if (chatId.isBlank()) return
        viewModelScope.launch {
            clearChatHistoryUseCase(chatId)
                .onSuccess {
                    _uiState.update { it.copy(userMessageRes = R.string.chat_info_msg_cleared) }
                    onCleared()
                }
                .onFailure { _uiState.update { it.copy(errorMessageRes = R.string.chat_info_error_clear_chat) } }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessageRes = null) }
    }
}
