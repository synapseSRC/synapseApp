package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.domain.usecase.ai.GenerateSmartRepliesUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatAiDelegate(
    private val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    private val summarizeChatUseCase: SummarizeChatUseCase,
    private val summarizeMessageUseCase: SummarizeMessageUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val participantProfileProvider: () -> User?,
    private val messageSuggestionEnabledProvider: () -> Boolean,
    private val onError: (String?) -> Unit,
    private val onLoading: (Boolean) -> Unit
) {

    val _smartReplies = MutableStateFlow<List<String>>(emptyList())
    val smartReplies: StateFlow<List<String>> = _smartReplies.asStateFlow()

    val _chatSummary = MutableStateFlow<String?>(null)
    val chatSummary: StateFlow<String?> = _chatSummary.asStateFlow()

    val _messageSummary = MutableStateFlow<String?>(null)
    val messageSummary: StateFlow<String?> = _messageSummary.asStateFlow()

    val _isSummarizingMessage = MutableStateFlow(false)
    val isSummarizingMessage: StateFlow<Boolean> = _isSummarizingMessage.asStateFlow()

    fun generateSmartReplies(currentMessages: List<Message>) {
        viewModelScope.launch {
            if (!messageSuggestionEnabledProvider()) {
                _smartReplies.value = emptyList()
                return@launch
            }

            if (currentMessages.isEmpty()) {
                _smartReplies.value = emptyList()
                return@launch
            }

            // Only consider recent messages to avoid prompt limits
            val recentMessages = currentMessages.takeLast(10).map { msg ->
                val senderName = if (msg.senderId == currentUserIdProvider()) "Me" else participantProfileProvider()?.displayName ?: "Them"
                "$senderName: ${msg.content}"
            }

            generateSmartRepliesUseCase(recentMessages)
                .onSuccess { replies ->
                    _smartReplies.value = replies
                }
                .onFailure {
                    // Ignore errors for smart replies
                    _smartReplies.value = emptyList()
                }
        }
    }

    fun summarizeChat(currentMessages: List<Message>) {
        viewModelScope.launch {
            if (currentMessages.isEmpty()) {
                _chatSummary.value = "No messages to summarize."
                return@launch
            }

            onLoading(true)

            // Consider recent messages to avoid prompt limits
            val messagesToSummarize = currentMessages.takeLast(50).map { msg ->
                val senderName = if (msg.senderId == currentUserIdProvider()) "Me" else participantProfileProvider()?.displayName ?: "Them"
                "$senderName: ${msg.content}"
            }

            summarizeChatUseCase(messagesToSummarize)
                .onSuccess { summary ->
                    _chatSummary.value = summary
                    onLoading(false)
                }
                .onFailure { e ->
                    onError("Failed to summarize chat: ${e.message}")
                    onLoading(false)
                }
        }
    }

    fun clearSummary() {
        _chatSummary.value = null
    }

    fun summarizeMessage(content: String) {
        viewModelScope.launch {
            _isSummarizingMessage.value = true
            summarizeMessageUseCase(content)
                .onSuccess { _messageSummary.value = it }
                .onFailure { _messageSummary.value = "Failed to summarize: ${it.message}" }
            _isSummarizingMessage.value = false
        }
    }

    fun clearMessageSummary() {
        _messageSummary.value = null
    }
}
