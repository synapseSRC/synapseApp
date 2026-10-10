package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _members = MutableStateFlow<List<Pair<User, Boolean>>>(emptyList())
    val members: StateFlow<List<Pair<User, Boolean>>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _onlyAdminsCanMessage = MutableStateFlow(false)
    val onlyAdminsCanMessage: StateFlow<Boolean> = _onlyAdminsCanMessage.asStateFlow()


    private var currentChatId: String? = null

    val currentUserId: String?
        get() = chatRepository.getCurrentUserId()

    fun loadMembers(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            chatRepository.getGroupMembers(chatId).onSuccess { loadedMembers ->
                _members.value = loadedMembers.distinctBy { it.first.uid }
            }.onFailure {
                _error.value = "Failed to load group members"
            }
            chatRepository.getChatInfo(chatId).onSuccess { info ->
                _onlyAdminsCanMessage.value = info?.onlyAdminsCanMessage ?: false
            }

            _isLoading.value = false
        }
    }


    fun addMember(userId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.addGroupMembers(chatId, listOf(userId)).onSuccess {
                loadMembers(chatId) // Reload to get full user object
            }.onFailure {
                _error.value = "Failed to add member"
            }
            _isLoading.value = false
        }
    }

    fun removeMember(userId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.removeGroupMember(chatId, userId).onSuccess {
                _members.value = _members.value.filter { it.first.uid != userId }
            }.onFailure {
                _error.value = "Failed to remove member"
            }
            _isLoading.value = false
        }
    }


    fun promoteToAdmin(userId: String) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.promoteToAdmin(chatId, userId).onSuccess {
                loadMembers(chatId)
            }.onFailure {
                _error.value = "Failed to promote to admin"
            }
            _isLoading.value = false
        }
    }

    fun demoteAdmin(userId: String) {
        val chatId = currentChatId ?: return

        // Ensure at least one admin remains
        val admins = _members.value.filter { it.second }
        if (admins.size <= 1 && admins.any { it.first.uid == userId }) {
            _error.value = "Cannot remove the only admin"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.demoteAdmin(chatId, userId).onSuccess {
                loadMembers(chatId)
            }.onFailure {
                _error.value = "Failed to demote admin"
            }
            _isLoading.value = false
        }
    }

    fun leaveGroup(onLeft: () -> Unit) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.leaveGroup(chatId).onSuccess {
                onLeft()
            }.onFailure {
                _error.value = "Failed to leave group"
            }
            _isLoading.value = false
        }
    }

    fun toggleOnlyAdminsCanMessage(enabled: Boolean) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.toggleOnlyAdminsCanMessage(chatId, enabled).onSuccess {
                _onlyAdminsCanMessage.value = enabled
            }.onFailure {
                _error.value = "Failed to toggle setting"
                // Revert optimistic update by not setting _onlyAdminsCanMessage.value
            }
            _isLoading.value = false
        }
    }
}
