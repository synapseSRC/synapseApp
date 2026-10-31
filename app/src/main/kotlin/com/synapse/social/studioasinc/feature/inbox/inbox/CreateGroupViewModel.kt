package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.repository.GroupCreationRepository
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GroupCreationError {
    AUTH_REQUIRED, LOAD_CONTACTS, SEARCH_FAILED, NAME_REQUIRED, MEMBER_REQUIRED,
    CREATE_FAILED, INVITE_UNAVAILABLE, LOAD_MEMBERS_FAILED,
    LOAD_REQUESTS_FAILED, UPDATE_MEMBER_FAILED
}

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupCreationRepository: GroupCreationRepository,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase
) : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private var initialUsers = emptyList<User>()
    private var searchJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _selectedUsers = MutableStateFlow<List<User>>(emptyList())
    val selectedUsers: StateFlow<List<User>> = _selectedUsers.asStateFlow()
    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()
    private val _groupDescription = MutableStateFlow("")
    val groupDescription: StateFlow<String> = _groupDescription.asStateFlow()

    private val _options = MutableStateFlow(GroupCreationOptions())
    val options: StateFlow<GroupCreationOptions> = _options.asStateFlow()
    private val _selectedPreset = MutableStateFlow("collaborative")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()
    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()
    private val _error = MutableStateFlow<GroupCreationError?>(null)
    val error: StateFlow<GroupCreationError?> = _error.asStateFlow()

    private val _createdChatId = MutableStateFlow<String?>(null)
    val createdChatId: StateFlow<String?> = _createdChatId.asStateFlow()
    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()
    private val _members = MutableStateFlow<List<Pair<User, Boolean>>>(emptyList())
    val members: StateFlow<List<Pair<User, Boolean>>> = _members.asStateFlow()
    private val _pendingRequests = MutableStateFlow<List<GroupJoinRequest>>(emptyList())
    val pendingRequests: StateFlow<List<GroupJoinRequest>> = _pendingRequests.asStateFlow()
    private val _isLoadingRequests = MutableStateFlow(false)
    val isLoadingRequests: StateFlow<Boolean> = _isLoadingRequests.asStateFlow()
    private val _isUpdatingInvite = MutableStateFlow(false)
    val isUpdatingInvite: StateFlow<Boolean> = _isUpdatingInvite.asStateFlow()

    val currentUserId: String? get() = chatRepository.getCurrentUserId()

    init { loadUsers() }

    private fun loadUsers() {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            _error.value = null
            try {
                val userId = chatRepository.getCurrentUserId()
                if (userId == null) {
                    _error.value = GroupCreationError.AUTH_REQUIRED
                    return@launch
                }
                getFollowingUseCase(userId).onSuccess { result ->
                    initialUsers = result.distinctBy { it.uid }.filterNot { it.uid == userId }
                    if (_searchQuery.value.isBlank()) _users.value = initialUsers
                }.onFailure {
                    _error.value = GroupCreationError.LOAD_CONTACTS
                }
            } catch (_: Exception) {
                _error.value = GroupCreationError.LOAD_CONTACTS
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _users.value = initialUsers
            _isLoadingUsers.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoadingUsers.value = true
            _error.value = null
            try {
                searchUsersUseCase(query.trim()).onSuccess { result ->
                    _users.value = result.distinctBy { it.uid }.filterNot { it.uid == currentUserId }
                }.onFailure {
                    _error.value = GroupCreationError.SEARCH_FAILED
                }
            } catch (_: Exception) {
                _error.value = GroupCreationError.SEARCH_FAILED
            } finally {
                _isLoadingUsers.value = false
            }
        }
    }

    fun toggleUserSelection(user: User) {
        _error.value = null
        _selectedUsers.value = if (_selectedUsers.value.any { it.uid == user.uid }) {
            _selectedUsers.value.filterNot { it.uid == user.uid }
        } else {
            (_selectedUsers.value + user).distinctBy { it.uid }
        }
    }

    fun removeMember(user: User) {
        _selectedUsers.value = _selectedUsers.value.filterNot { it.uid == user.uid }
    }

    fun clearSelection() {
        _selectedUsers.value = emptyList()
    }

    fun updateGroupName(name: String) { _groupName.value = name.take(60) }
    fun updateGroupDescription(description: String) { _groupDescription.value = description.take(240) }
    fun clearError() { _error.value = null }

    fun selectPreset(preset: String) {
        _selectedPreset.value = preset
        _options.value = when (preset) {
            "announcement" -> _options.value.copy(sendMessagesByMembers = false, allowMemberSettingsEdits = false, requireJoinApproval = true, allowMemberInvites = false, disappearingMode = DisappearingMode.OFF)
            "private" -> _options.value.copy(sendMessagesByMembers = true, allowMemberSettingsEdits = false, requireJoinApproval = true, allowMemberInvites = false, disappearingMode = DisappearingMode.NINETY_DAYS)
            else -> _options.value.copy(sendMessagesByMembers = true, allowMemberSettingsEdits = false, requireJoinApproval = true, allowMemberInvites = false, disappearingMode = DisappearingMode.OFF)
        }
    }

    fun updateSendMessages(enabled: Boolean) { _options.value = _options.value.copy(sendMessagesByMembers = enabled) }
    fun updateMemberSettingsEdits(enabled: Boolean) { _options.value = _options.value.copy(allowMemberSettingsEdits = enabled) }
    fun updateJoinApproval(enabled: Boolean) { _options.value = _options.value.copy(requireJoinApproval = enabled) }
    fun updateMemberInvites(enabled: Boolean) { _options.value = _options.value.copy(allowMemberInvites = enabled) }
    fun updateDisappearingMode(mode: DisappearingMode) { _options.value = _options.value.copy(disappearingMode = mode) }

    fun createGroup() {
        val name = _groupName.value.trim()
        if (name.isBlank()) { _error.value = GroupCreationError.NAME_REQUIRED; return }
        if (_selectedUsers.value.isEmpty()) { _error.value = GroupCreationError.MEMBER_REQUIRED; return }
        if (_isCreating.value) return

        viewModelScope.launch {
            _isCreating.value = true
            _error.value = null
            try {
                val options = _options.value.copy(description = _groupDescription.value.trim().takeIf { it.isNotBlank() })
                groupCreationRepository.createGroupChat(
                    name = name,
                    participantIds = _selectedUsers.value.map { it.uid }.distinct(),
                    options = options
                ).onSuccess { chatId ->
                    _createdChatId.value = chatId
                    groupCreationRepository.getGroupInviteCode(chatId).onSuccess { _inviteCode.value = it }
                        .onFailure { _error.value = GroupCreationError.INVITE_UNAVAILABLE }
                    loadGroupMembers(chatId)
                    loadPendingRequests(chatId)
                }.onFailure {
                    _error.value = GroupCreationError.CREATE_FAILED
                }
            } catch (_: Exception) {
                _error.value = GroupCreationError.CREATE_FAILED
            } finally {
                _isCreating.value = false
            }
        }
    }

    private suspend fun loadGroupMembers(chatId: String) {
        chatRepository.getGroupMembers(chatId).onSuccess { _members.value = it.distinctBy { member -> member.first.uid } }
            .onFailure { _error.value = GroupCreationError.LOAD_MEMBERS_FAILED }
    }

    private suspend fun loadPendingRequests(chatId: String) {
        _isLoadingRequests.value = true
        groupCreationRepository.getPendingGroupJoinRequests(chatId).onSuccess { _pendingRequests.value = it }
            .onFailure { _error.value = GroupCreationError.LOAD_REQUESTS_FAILED }
        _isLoadingRequests.value = false
    }

    fun refreshPendingRequests() {
        val chatId = _createdChatId.value ?: return
        viewModelScope.launch { loadPendingRequests(chatId) }
    }

    fun regenerateInvite() {
        val chatId = _createdChatId.value ?: return
        if (_isUpdatingInvite.value) return
        viewModelScope.launch {
            _isUpdatingInvite.value = true
            _error.value = null
            groupCreationRepository.rotateGroupInviteCode(chatId).onSuccess { _inviteCode.value = it }
                .onFailure { _error.value = GroupCreationError.INVITE_UNAVAILABLE }
            _isUpdatingInvite.value = false
        }
    }

    fun approveRequest(requestId: String) = updateRequest(requestId, approve = true)
    fun declineRequest(requestId: String) = updateRequest(requestId, approve = false)

    private fun updateRequest(requestId: String, approve: Boolean) {
        val chatId = _createdChatId.value ?: return
        viewModelScope.launch {
            val result = if (approve) groupCreationRepository.approveGroupJoinRequest(requestId)
                else groupCreationRepository.declineGroupJoinRequest(requestId)
            result.onSuccess {
                loadPendingRequests(chatId)
                if (approve) loadGroupMembers(chatId)
            }.onFailure { _error.value = GroupCreationError.LOAD_REQUESTS_FAILED }
        }
    }

    fun changeMemberRole(userId: String, makeAdmin: Boolean) {
        val chatId = _createdChatId.value ?: return
        viewModelScope.launch {
            val result = if (makeAdmin) chatRepository.promoteToAdmin(chatId, userId)
                else chatRepository.demoteAdmin(chatId, userId)
            result.onSuccess { loadGroupMembers(chatId) }
                .onFailure { _error.value = GroupCreationError.UPDATE_MEMBER_FAILED }
        }
    }

    fun removeGroupMember(userId: String) {
        val chatId = _createdChatId.value ?: return
        viewModelScope.launch {
            chatRepository.removeGroupMember(chatId, userId).onSuccess { loadGroupMembers(chatId) }
                .onFailure { _error.value = GroupCreationError.UPDATE_MEMBER_FAILED }
        }
    }
}
