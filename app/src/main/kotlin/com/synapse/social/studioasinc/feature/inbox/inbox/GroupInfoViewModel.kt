package com.synapse.social.studioasinc.feature.inbox.inbox

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.GroupCreationRepository
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.follow.GetFollowingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.user.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Single source of truth for the group info screen.
 *
 * Errors and success notices are carried as string-resource ids (`errorRes` / `successMessageRes`)
 * so the presentation layer stays free of hardcoded copy.
 */
data class GroupInfoUiState(
    val isLoading: Boolean = true,
    val groupInfo: ChatInfo? = null,
    val members: List<Pair<User, Boolean>> = emptyList(),
    val pendingRequests: List<GroupJoinRequest> = emptyList(),
    val inviteCode: String? = null,
    val isMuted: Boolean = false,
    val isMuteUpdating: Boolean = false,
    val currentUserIsAdmin: Boolean = false,
    val currentUserIsCreator: Boolean = false,
    val currentUserId: String? = null,
    val errorRes: Int? = null,
    val successMessageRes: Int? = null,
    val memberSearchQuery: String = "",
    val filteredMembers: List<Pair<User, Boolean>> = emptyList(),
    /** True when the current user may edit group info: admins always, members only when the group allows it. */
    val canEditGroupInfo: Boolean = false,
    val isAvatarUploading: Boolean = false,
    val addableUsers: List<User> = emptyList(),
    val selectedUsersToAdd: List<User> = emptyList(),
    val addMemberSearchQuery: String = "",
    val isLoadingContacts: Boolean = false
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val groupCreationRepository: GroupCreationRepository,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupInfoUiState())
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private var currentChatId: String? = null
    private var addMemberSearchJob: Job? = null
    private var addableContactsCache = emptyList<User>()

    // region Loading

    /** Loads group info, members, invite code, mute status and pending join requests in parallel. */
    fun loadGroupInfo(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorRes = null)
            val userId = chatRepository.getCurrentUserId()

            val infoDeferred = async { chatRepository.getChatInfo(chatId) }
            val membersDeferred = async { chatRepository.getGroupMembers(chatId) }
            val inviteDeferred = async { groupCreationRepository.getGroupInviteCode(chatId) }
            val muteDeferred = async { chatRepository.getConversationMuteStatus(chatId) }
            val requestsDeferred = async { groupCreationRepository.getPendingGroupJoinRequests(chatId) }

            val info = infoDeferred.await().getOrNull()
            val members = membersDeferred.await().getOrNull()?.distinctBy { it.first.uid } ?: emptyList()
            val inviteCode = inviteDeferred.await().getOrNull()
            val isMuted = muteDeferred.await().getOrNull() ?: false
            val requests = requestsDeferred.await().getOrNull() ?: emptyList()

            val createdBy = info?.createdBy
            val isCreator = createdBy != null && createdBy == userId
            val isAdmin = members.find { it.first.uid == userId }?.second == true || isCreator

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                groupInfo = info,
                members = members,
                filteredMembers = members,
                pendingRequests = requests,
                inviteCode = inviteCode,
                isMuted = isMuted,
                currentUserIsAdmin = isAdmin,
                currentUserIsCreator = isCreator,
                currentUserId = userId,
                canEditGroupInfo = isAdmin || info?.allowMemberSettingsEdits == true
            )
        }
    }

    /**
     * Loads only what the participants screen renders — group info (for the creator id) and the roster —
     * so opening that screen doesn't pay for the invite code, mute status or request queue.
     */
    fun loadParticipants(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorRes = null)
            val userId = chatRepository.getCurrentUserId()
            val info = chatRepository.getChatInfo(chatId).getOrNull()
            val members = chatRepository.getGroupMembers(chatId).getOrNull()?.distinctBy { it.first.uid } ?: emptyList()
            val isCreator = info?.createdBy != null && info.createdBy == userId
            val isAdmin = members.find { it.first.uid == userId }?.second == true || isCreator

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                groupInfo = info,
                members = members,
                filteredMembers = members,
                currentUserIsAdmin = isAdmin,
                currentUserIsCreator = isCreator,
                currentUserId = userId,
                canEditGroupInfo = isAdmin || info?.allowMemberSettingsEdits == true
            )
        }
    }

    /** Re-reads the member roster and re-applies the active search filter. */
    private suspend fun reloadMembers(chatId: String) {
        chatRepository.getGroupMembers(chatId).onSuccess { loaded ->
            val distinct = loaded.distinctBy { it.first.uid }
            val state = _uiState.value
            val nowAdmin = distinct.find { it.first.uid == state.currentUserId }?.second == true ||
                state.currentUserIsCreator
            _uiState.value = state.copy(
                members = distinct,
                currentUserIsAdmin = nowAdmin,
                canEditGroupInfo = nowAdmin || state.groupInfo?.allowMemberSettingsEdits == true
            )
            filterMembers()
        }
    }

    /** Re-reads the pending join request queue. */
    private suspend fun reloadPendingRequests(chatId: String) {
        groupCreationRepository.getPendingGroupJoinRequests(chatId)
            .onSuccess { requests ->
                _uiState.value = _uiState.value.copy(pendingRequests = requests)
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_requests)
            }
    }

    // endregion

    // region Group profile edits

    fun updateGroupName(name: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.updateGroupInfo(chatId, name = name)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(groupInfo = updated, errorRes = null)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun updateGroupDescription(description: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.updateGroupInfo(chatId, description = description)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(groupInfo = updated, errorRes = null)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun updateGroupAvatar(avatarUrl: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.updateGroupInfo(chatId, avatarUrl = avatarUrl)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(groupInfo = updated, errorRes = null)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    /**
     * Copies the picked image [uri] into the cache, uploads it to the `group-avatars` bucket and,
     * on success, persists the resulting URL as the group avatar (encrypted fan-out in the repository).
     */
    fun onGroupAvatarPicked(uri: Uri) {
        val chatId = currentChatId ?: return
        if (_uiState.value.isAvatarUploading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAvatarUploading = true, errorRes = null)
            val file = copyUriToCache(uri)
            if (file == null) {
                _uiState.value = _uiState.value.copy(isAvatarUploading = false, errorRes = R.string.group_avatar_upload_failed)
                return@launch
            }
            uploadMediaUseCase(
                filePath = file.absolutePath,
                mediaType = MediaType.IMAGE,
                bucketName = GROUP_AVATAR_BUCKET,
                requirePrivateStorage = true,
                onProgress = { }
            ).onSuccess { result ->
                _uiState.value = _uiState.value.copy(isAvatarUploading = false)
                chatRepository.updateGroupInfo(chatId, avatarUrl = result.url)
                    .onSuccess { updated -> _uiState.value = _uiState.value.copy(groupInfo = updated, errorRes = null) }
                    .onFailure { _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create) }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isAvatarUploading = false, errorRes = R.string.group_avatar_upload_failed)
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File? = try {
        val dest = File(context.cacheDir, "group_avatar_${System.currentTimeMillis()}.jpg")
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream -> dest.outputStream().use { output -> stream.copyTo(output) } }
        dest
    } catch (e: Exception) {
        null
    }

    // endregion

    // region Group settings toggles

    fun toggleOnlyAdminsCanMessage(enabled: Boolean) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.toggleOnlyAdminsCanMessage(chatId, enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        groupInfo = _uiState.value.groupInfo?.copy(onlyAdminsCanMessage = enabled),
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun toggleAllowMemberInvites(enabled: Boolean) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.updateGroupPrivacy(chatId, allowMemberInvites = enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        groupInfo = _uiState.value.groupInfo?.copy(allowMemberInvites = enabled),
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun toggleRequireJoinApproval(enabled: Boolean) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.updateGroupPrivacy(chatId, requireJoinApproval = enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        groupInfo = _uiState.value.groupInfo?.copy(requireJoinApproval = enabled),
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun toggleAllowMemberSettingsEdits(enabled: Boolean) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.updateGroupPrivacy(chatId, allowMemberSettingsEdits = enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        groupInfo = _uiState.value.groupInfo?.copy(allowMemberSettingsEdits = enabled),
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun setDisappearingMode(mode: DisappearingMode) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.setDisappearingMode(chatId, mode)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        groupInfo = _uiState.value.groupInfo?.copy(disappearingMode = mode),
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun toggleMute() {
        val chatId = currentChatId ?: return
        val newMuted = !_uiState.value.isMuted
        _uiState.value = _uiState.value.copy(isMuteUpdating = true)
        viewModelScope.launch {
            try {
                chatRepository.setConversationMuteStatus(chatId, newMuted)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isMuted = newMuted, errorRes = null)
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                    }
            } finally {
                _uiState.value = _uiState.value.copy(isMuteUpdating = false)
            }
        }
    }

    // endregion

    // region Invite link

    fun rotateInviteCode() {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            groupCreationRepository.rotateGroupInviteCode(chatId)
                .onSuccess { code ->
                    _uiState.value = _uiState.value.copy(inviteCode = code, errorRes = null)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_invite)
                }
        }
    }

    // endregion

    // region Members

    fun addMember(userId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.addGroupMembers(chatId, listOf(userId))
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorRes = null)
                    reloadMembers(chatId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_member_update)
                }
        }
    }

    /** Loads the following list, minus current members and self, as the add-member picker source. */
    fun loadAddableContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContacts = true)
            val userId = chatRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoadingContacts = false)
                return@launch
            }
            getFollowingUseCase(userId).onSuccess { result ->
                val memberIds = _uiState.value.members.map { it.first.uid }.toSet()
                addableContactsCache = result.distinctBy { it.uid }
                    .filterNot { it.uid == userId || memberIds.contains(it.uid) }
                _uiState.value = _uiState.value.copy(
                    isLoadingContacts = false,
                    addableUsers = addableContactsCache,
                    addMemberSearchQuery = "",
                    selectedUsersToAdd = emptyList()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingContacts = false, errorRes = R.string.group_error_member_update)
            }
        }
    }

    /** Debounced user search for the add-member picker; blank query restores the following list. */
    fun onAddMemberSearch(query: String) {
        _uiState.value = _uiState.value.copy(addMemberSearchQuery = query)
        addMemberSearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(addableUsers = addableContactsCache, isLoadingContacts = false)
            return
        }
        addMemberSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isLoadingContacts = true)
            searchUsersUseCase(query.trim()).onSuccess { result ->
                val userId = chatRepository.getCurrentUserId()
                val memberIds = _uiState.value.members.map { it.first.uid }.toSet()
                _uiState.value = _uiState.value.copy(
                    isLoadingContacts = false,
                    addableUsers = result.distinctBy { it.uid }
                        .filterNot { it.uid == userId || memberIds.contains(it.uid) }
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingContacts = false)
            }
        }
    }

    fun toggleAddMemberSelection(user: User) {
        val current = _uiState.value.selectedUsersToAdd
        _uiState.value = _uiState.value.copy(
            selectedUsersToAdd = if (current.any { it.uid == user.uid }) {
                current.filterNot { it.uid == user.uid }
            } else {
                (current + user).distinctBy { it.uid }
            }
        )
    }

    fun removeAddMemberSelection(user: User) {
        _uiState.value = _uiState.value.copy(
            selectedUsersToAdd = _uiState.value.selectedUsersToAdd.filterNot { it.uid == user.uid }
        )
    }

    fun clearAddMemberSelection() {
        _uiState.value = _uiState.value.copy(selectedUsersToAdd = emptyList())
    }

    /** Adds every selected user, then reloads the roster. [onDone] runs on success to dismiss the sheet. */
    fun confirmAddMembers(onDone: () -> Unit = {}) {
        val chatId = currentChatId ?: return
        val ids = _uiState.value.selectedUsersToAdd.map { it.uid }.distinct()
        if (ids.isEmpty()) { onDone(); return }
        viewModelScope.launch {
            chatRepository.addGroupMembers(chatId, ids)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(selectedUsersToAdd = emptyList(), errorRes = null)
                    reloadMembers(chatId)
                    onDone()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_member_update)
                }
        }
    }

    fun removeMember(userId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.removeGroupMember(chatId, userId)
                .onSuccess {
                    val remaining = _uiState.value.members.filterNot { it.first.uid == userId }
                    _uiState.value = _uiState.value.copy(
                        members = remaining,
                        filteredMembers = _uiState.value.filteredMembers.filterNot { it.first.uid == userId },
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_member_update)
                }
        }
    }

    fun promoteToAdmin(userId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.promoteToAdmin(chatId, userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorRes = null)
                    reloadMembers(chatId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_member_update)
                }
        }
    }

    fun demoteAdmin(userId: String) {
        val chatId = currentChatId ?: return

        // Client-side guard: a group must always keep at least one admin.
        val admins = _uiState.value.members.filter { it.second }
        if (admins.size <= 1 && admins.any { it.first.uid == userId }) {
            _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_member_update)
            return
        }

        viewModelScope.launch {
            chatRepository.demoteAdmin(chatId, userId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorRes = null)
                    reloadMembers(chatId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_member_update)
                }
        }
    }

    // endregion

    // region Join requests

    fun approveRequest(requestId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            groupCreationRepository.approveGroupJoinRequest(requestId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(errorRes = null)
                    reloadMembers(chatId)
                    reloadPendingRequests(chatId)
                    // Re-fan-out encrypted metadata so the newly approved member can decrypt it.
                    chatRepository.resyncGroupMetadata(chatId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_requests)
                }
        }
    }

    fun declineRequest(requestId: String) {
        // declineGroupJoinRequest keys off the request id only; guard against acting before load.
        if (currentChatId == null) return
        viewModelScope.launch {
            groupCreationRepository.declineGroupJoinRequest(requestId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        pendingRequests = _uiState.value.pendingRequests.filterNot { it.id == requestId },
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_requests)
                }
        }
    }

    // endregion

    // region Destructive actions

    fun leaveGroup(onLeft: () -> Unit) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.leaveGroup(chatId)
                .onSuccess { onLeft() }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun deleteGroup(onDeleted: () -> Unit) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.deleteGroup(chatId)
                .onSuccess { onDeleted() }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    fun reportGroup(reason: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.reportChat(chatId, reason)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        successMessageRes = R.string.group_report_success,
                        errorRes = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(errorRes = R.string.group_error_create)
                }
        }
    }

    // endregion

    // region Member search

    fun onMemberSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(memberSearchQuery = query)
        filterMembers()
    }

    private fun filterMembers() {
        val query = _uiState.value.memberSearchQuery.lowercase().trim()
        val filtered = if (query.isBlank()) {
            _uiState.value.members
        } else {
            _uiState.value.members.filter { (user, _) ->
                user.displayName?.lowercase()?.contains(query) == true ||
                    user.username?.lowercase()?.contains(query) == true
            }
        }
        _uiState.value = _uiState.value.copy(filteredMembers = filtered)
    }

    // endregion

    // region One-shot message consumption

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorRes = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessageRes = null)
    }

    // endregion

    private companion object {
        const val GROUP_AVATAR_BUCKET = "group-avatars"
    }
}
