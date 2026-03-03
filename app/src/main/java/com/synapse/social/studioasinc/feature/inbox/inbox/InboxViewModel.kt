package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    // private val getConversationsUseCase: GetConversationsUseCase,
    // private val subscribeToInboxUpdatesUseCase: SubscribeToInboxUpdatesUseCase
) : ViewModel() {

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()

    private val _conversations = MutableStateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>>(emptyList())
    val conversations: StateFlow<List<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            loadCurrentUserProfile()
            loadConversations()
            subscribeToInboxUpdates()
        }
    }

    private suspend fun loadCurrentUserProfile() {
        try {
            val profile = UserProfileManager.getCurrentUserProfile()
            _currentUserProfile.value = profile
        } catch (_: Exception) { }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Mock Data
            _conversations.value = listOf(
                Conversation(
                    chatId = "mock-chat-1",
                    participantId = "alice",
                    participantName = "Alice",
                    participantAvatar = "https://i.pravatar.cc/150?u=alice",
                    lastMessage = "Hello Alice! Good to see you.",
                    lastMessageTime = Instant.now().minusSeconds(1800).toString(),
                    unreadCount = 0,
                    isOnline = true
                )
            )

            _isLoading.value = false
        }
    }

    private fun subscribeToInboxUpdates() {
        // Mock: no-op
    }

    fun getFormattedTimestamp(timestamp: String?): String = TimestampFormatter.formatRelative(timestamp)
}
