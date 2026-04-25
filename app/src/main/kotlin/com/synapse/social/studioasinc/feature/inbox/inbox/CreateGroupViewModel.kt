package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _selectedUsers = MutableStateFlow<List<User>>(emptyList())
    val selectedUsers: StateFlow<List<User>> = _selectedUsers.asStateFlow()

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successChatId = MutableStateFlow<String?>(null)
    val successChatId: StateFlow<String?> = _successChatId.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            // Load followers/following or all users. Just mock or use an existing use case.
            // Let's use search users or just load some users.
            try {
                // Dummy logic for now since getting all users might need a specific usecase
                // We'll just assume userRepository.getSuggestedUsers or similar
                val currentUserId = chatRepository.getCurrentUserId() ?: return@launch
                // Let's just use search to get some users since getFollowers doesn't exist
                userRepository.searchUsers("").onSuccess { result ->
                    _users.value = result
                }.onFailure {
                    _error.value = "Failed to load users"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleUserSelection(user: User) {
        val current = _selectedUsers.value.toMutableList()
        if (current.any { it.uid == user.uid }) {
            current.removeAll { it.uid == user.uid }
        } else {
            current.add(user)
        }
        _selectedUsers.value = current
    }

    fun removeMember(user: User) {
        val current = _selectedUsers.value.toMutableList()
        current.removeAll { it.uid == user.uid }
        _selectedUsers.value = current
    }

    fun updateGroupName(name: String) {
        _groupName.value = name
    }

    fun createGroup() {
        if (_groupName.value.isBlank()) {
            _error.value = "Group name cannot be empty"
            return
        }
        if (_selectedUsers.value.isEmpty()) {
            _error.value = "Select at least one member"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val participantIds = _selectedUsers.value.map { it.uid }

            chatRepository.createGroupChat(_groupName.value, participantIds, null).onSuccess { chatId ->
                _successChatId.value = chatId
            }.onFailure {
                _error.value = "Failed to create group"
            }

            _isLoading.value = false
        }
    }
}
