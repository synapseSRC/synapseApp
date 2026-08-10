package com.synapse.social.studioasinc.desktop.ui

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

enum class ConversationFilter {
    ALL, UNREAD, FAVOURITES
}

class DesktopChatViewModel(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val authRepository: AuthRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(ConversationFilter.ALL)
    val activeFilter: StateFlow<ConversationFilter> = _activeFilter.asStateFlow()

    val currentUserId: String? = authRepository.getCurrentUserId()

    val filteredConversations: StateFlow<List<Conversation>> = combine(
        _conversations,
        _searchQuery,
        _activeFilter
    ) { convs, query, filter ->
        var list = convs
        list = when (filter) {
            ConversationFilter.ALL -> list
            ConversationFilter.UNREAD -> list.filter { it.unreadCount > 0 }
            ConversationFilter.FAVOURITES -> list // TODO: Implement favourites filter
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.participantName.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _isLoadingConversations.value = true
            _error.value = null
            getConversationsUseCase().onSuccess { result ->
                _conversations.value = result
            }.onFailure { error ->
                Napier.e("Failed to load conversations", error)
                _error.value = "Failed to load conversations: ${error.message}"
            }
            _isLoadingConversations.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ConversationFilter) {
        _activeFilter.value = filter
    }

    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        loadMessages(conversation.chatId)
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _isLoadingMessages.value = true
            _error.value = null
            getMessagesUseCase(chatId = chatId).onSuccess { result ->
                _messages.value = result
            }.onFailure { error ->
                Napier.e("Failed to load messages", error)
                _messages.value = emptyList()
                _error.value = "Failed to load messages: ${error.message}"
            }
            _isLoadingMessages.value = false
        }
    }

    fun sendMessage(content: String) {
        val chatId = _selectedConversation.value?.chatId ?: return
        viewModelScope.launch {
            _error.value = null
            sendMessageUseCase(
                chatId = chatId,
                content = content
            ).onSuccess {
                // Optimistically reload messages for now, or just append it if we have real-time
                loadMessages(chatId)
            }.onFailure { error ->
                Napier.e("Failed to send message", error)
                _error.value = "Failed to send message: ${error.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
