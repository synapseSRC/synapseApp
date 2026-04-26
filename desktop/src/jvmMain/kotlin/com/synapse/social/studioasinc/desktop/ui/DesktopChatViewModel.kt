package com.synapse.social.studioasinc.desktop.ui

import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay

class DesktopChatViewModel(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _isLoadingConversations.value = true
            getConversationsUseCase().onSuccess { result ->
                _conversations.value = result
            }.onFailure { error ->
                Napier.e("Failed to load conversations", error)
            }
            _isLoadingConversations.value = false
        }
    }

    fun selectConversation(conversation: Conversation) {
        _selectedConversation.value = conversation
        loadMessages(conversation.chatId)
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _isLoadingMessages.value = true
            getMessagesUseCase(chatId = chatId).onSuccess { result ->
                _messages.value = result
            }.onFailure { error ->
                Napier.e("Failed to load messages", error)
                _messages.value = emptyList()
            }
            _isLoadingMessages.value = false
        }
    }

    fun sendMessage(content: String) {
        val chatId = _selectedConversation.value?.chatId ?: return
        viewModelScope.launch {
            sendMessageUseCase(
                chatId = chatId,
                content = content
            ).onSuccess {
                // Optimistically reload messages for now, or just append it if we have real-time
                loadMessages(chatId)
            }.onFailure { error ->
                Napier.e("Failed to send message", error)
            }
        }
    }
}
