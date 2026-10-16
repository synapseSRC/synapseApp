package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class ChatInputDelegate(
    private val broadcastTypingStatusUseCase: BroadcastTypingStatusUseCase,
    private val viewModelScope: CoroutineScope,
    private val messagingDelegate: ChatMessagingDelegate,
    private val _inputText: MutableStateFlow<String>,
    private val _editingMessage: MutableStateFlow<Message?>,
    private val _error: MutableStateFlow<String?>,
    private val _replyingToMessage: MutableStateFlow<Message?>,
    private val _toastMessage: MutableStateFlow<String?>,
    private val _selectedMessageIds: MutableStateFlow<Set<String>>,
    private val chatMaxMessageChunkSizeProvider: () -> Int,
    private val currentChatIdProvider: () -> String?,
    private val isE2EEReadyProvider: () -> Boolean,
    private val disappearingModeProvider: () -> DisappearingMode,
    private val onChatRefreshRequired: () -> Unit
) {
    private var typingDebounceJob: Job? = null
    private var isCurrentlyTyping = false
    private val sendQueueMutexes = ConcurrentHashMap<String, Mutex>()

    fun onInputTextChange(newText: String) {
        _inputText.value = newText

        val chatId = currentChatIdProvider() ?: return

        if (!isCurrentlyTyping) {
            isCurrentlyTyping = true
            viewModelScope.launch {
                broadcastTypingStatusUseCase(chatId, true)
            }
        }

        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch {
            delay(3000)
            broadcastTypingStatusUseCase(chatId, false)
            isCurrentlyTyping = false
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val chatId = currentChatIdProvider() ?: return
        if (text.isEmpty()) return

        val editingMsg = _editingMessage.value
        if (editingMsg != null) {
            messagingDelegate.saveEdit(
                message = editingMsg,
                newContent = text,
                onError = { _error.value = "Failed to edit: $it" },
                onSuccess = {
                    _editingMessage.value = null
                    _inputText.value = ""
                }
            )
            return
        }

        val chunks = messagingDelegate.splitIntoChunks(text, chatMaxMessageChunkSizeProvider())
        val currentMode = disappearingModeProvider()
        val expiresAt = currentMode.seconds?.let { seconds ->
            java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .plusSeconds(seconds)
                .format(java.time.format.DateTimeFormatter.ISO_INSTANT)
        }
        val replyToId = _replyingToMessage.value?.id

        // Consume this draft now; repeated taps cannot enqueue it twice, and later typing or
        // reply selection is not cleared when this send eventually leaves the E2EE queue.
        _replyingToMessage.value = null
        _inputText.value = ""

        viewModelScope.launch {
            sendQueueMutexes.computeIfAbsent(chatId) { Mutex() }.withLock {
                if (!isE2EEReadyProvider()) {
                    Napier.d("E2EE not ready, waiting for initialization...", tag = "E2EE")
                    var attempts = 0
                    while (!isE2EEReadyProvider() && attempts < 30) {
                        delay(100)
                        attempts++
                    }
                    if (isE2EEReadyProvider()) {
                        Napier.d("E2EE ready after waiting", tag = "E2EE")
                    } else {
                        Napier.w("E2EE initialization timeout, proceeding anyway", tag = "E2EE")
                    }
                }

                sendChunks(chatId, chunks, expiresAt, replyToId)
            }
        }
    }

    private fun sendChunks(
        chatId: String,
        chunks: List<String>,
        expiresAt: String?,
        replyToId: String?
    ) {
        chunks.forEach { chunk ->
            messagingDelegate.performSendMessage(
                chatId = chatId,
                text = chunk,
                expiresAt = expiresAt,
                replyToId = replyToId,
                onError = { error ->
                    _error.value = "Failed to send: $error"
                    _toastMessage.value = error ?: "Failed to send message"
                }
            )
        }
    }

    fun startEditing(message: Message) {
        _editingMessage.value = message
        _inputText.value = message.content ?: ""
    }

    fun cancelEditing() {
        _editingMessage.value = null
        _inputText.value = ""
    }

    fun toggleMessageSelection(messageId: String) {
        _selectedMessageIds.update { current ->
            if (current.contains(messageId)) {
                current - messageId
            } else {
                current + messageId
            }
        }
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun deleteSelectedMessages() {
        val selectedIds = _selectedMessageIds.value.toList()
        _selectedMessageIds.value = emptySet()

        messagingDelegate.deleteSelectedMessages(
            selectedIds = selectedIds,
            onError = { _error.value = "Failed to delete messages: $it" },
            onRequiresRefresh = onChatRefreshRequired
        )
    }

    fun setReplyingToMessage(message: Message) {
        _replyingToMessage.value = message
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    fun cleanup() {
        typingDebounceJob?.cancel()
        typingDebounceJob = null
    }
}
