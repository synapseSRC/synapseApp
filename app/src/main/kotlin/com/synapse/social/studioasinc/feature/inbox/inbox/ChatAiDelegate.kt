package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.usecase.ai.GenerateSmartRepliesUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class ChatAiDelegate(
    private val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    private val summarizeChatUseCase: SummarizeChatUseCase,
    private val summarizeMessageUseCase: SummarizeMessageUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val chatIdProvider: () -> String?,
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

    private var smartRepliesJob: Job? = null
    private var smartRepliesGeneration = 0L
    private val activeJobs = ConcurrentHashMap<Job, Unit>()

    fun cancelPendingSmartReplies() {
        smartRepliesGeneration++
        smartRepliesJob?.cancel()
        smartRepliesJob = null
        _smartReplies.value = emptyList()
    }

    fun generateSmartReplies(currentMessages: List<Message>) {
        smartRepliesJob?.cancel()
        val generation = ++smartRepliesGeneration
        val chatId = chatIdProvider()

        if (!messageSuggestionEnabledProvider() || currentMessages.isEmpty()) {
            _smartReplies.value = emptyList()
            return
        }

        // Snapshot only the newest context. Realtime bursts cancel stale generations.
        val recentMessages = currentMessages.takeLast(10).map { msg ->
            val senderName = if (msg.senderId == currentUserIdProvider()) "Me" else participantProfileProvider()?.displayName ?: "Them"
            "$senderName: ${msg.content}"
        }

        smartRepliesJob = viewModelScope.launch {
            delay(SMART_REPLY_DEBOUNCE_MS)
            val result = generateSmartRepliesUseCase(recentMessages, chatId)
            if (generation != smartRepliesGeneration || chatId != chatIdProvider()) return@launch
            result.onSuccess { replies ->
                _smartReplies.value = replies
            }.onFailure {
                _smartReplies.value = emptyList()
            }
        }
        activeJobs[smartRepliesJob!!] = Unit
        smartRepliesJob!!.invokeOnCompletion { activeJobs.remove(smartRepliesJob!!) }
    }

    private companion object {
        const val SMART_REPLY_DEBOUNCE_MS = 350L
    }

    fun summarizeChat(currentMessages: List<Message>) {
        val job = viewModelScope.launch {
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

            summarizeChatUseCase(messagesToSummarize, chatIdProvider())
                .onSuccess { summary ->
                    _chatSummary.value = summary
                    onLoading(false)
                }
                .onFailure { e ->
                    onError("Failed to summarize chat: ${e.message}")
                    onLoading(false)
                }
        }
        activeJobs[job] = Unit
        job.invokeOnCompletion { activeJobs.remove(job) }
    }

    fun clearSummary() {
        _chatSummary.value = null
    }

    fun summarizeMessage(content: String) {
        val job = viewModelScope.launch {
            _isSummarizingMessage.value = true
            summarizeMessageUseCase(content, chatIdProvider())
                .onSuccess { _messageSummary.value = it }
                .onFailure { _messageSummary.value = "Failed to summarize: ${it.message}" }
            _isSummarizingMessage.value = false
        }
        activeJobs[job] = Unit
        job.invokeOnCompletion { activeJobs.remove(job) }
    }

    fun clearMessageSummary() {
        _messageSummary.value = null
    }

    fun cleanup() {
        cancelPendingSmartReplies()
        activeJobs.keys.forEach { it.cancel() }
        activeJobs.clear()
    }
}
