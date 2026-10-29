package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase
import android.os.SystemClock
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    private val typingIndicatorsEnabledProvider: () -> Boolean = { true },
    private val isScreenVisibleProvider: () -> Boolean = { true },
    private val onSendDispatched: () -> Unit = {}
) {
    // A stop is matched by identity, so new input cannot inherit an in-flight attempt.
    private class PendingStop {
        var attempts = 0
        var retryAt = 0L
    }

    private val typingWakeups = Channel<Unit>(Channel.CONFLATED)
    private var typingChatId: String? = null
    private var lastInputActivityAt = 0L
    private var nextHeartbeatAt = 0L
    private var screenVisible = true
    private val pendingStops = linkedMapOf<String, PendingStop>()
    private val possiblyTypingChats = linkedSetOf<String>()
    private val e2eeWaitExhaustedChats = ConcurrentHashMap.newKeySet<String>()

    init {
        // One scope-owned writer serializes true/false so a retry can never reorder behind its stop.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                while (isActive) {
                    val now = SystemClock.elapsedRealtime()
                    typingChatId?.let { chatId ->
                        if (!canBroadcastTyping() || currentChatIdProvider()?.equals(chatId, ignoreCase = true) != true ||
                            now - lastInputActivityAt >= TYPING_IDLE_MS
                        ) {
                            stopTyping()
                        }
                    }

                    val stop = pendingStops.entries.firstOrNull { it.value.retryAt <= now }
                        ?.let { it.key to it.value }
                    if (stop != null) {
                        val (chatId, request) = stop
                        val succeeded = broadcastTyping(chatId, false)
                        if (pendingStops[chatId] === request) {
                            val attempts = request.attempts + 1
                            if (succeeded || attempts >= STOP_ATTEMPTS) {
                                pendingStops.remove(chatId)
                            } else {
                                request.attempts = attempts
                                request.retryAt = SystemClock.elapsedRealtime() + STOP_RETRY_MS * attempts
                            }
                        }
                        if (succeeded) possiblyTypingChats.remove(chatId)
                        continue
                    }

                    val chatId = typingChatId
                    if (chatId != null && now >= nextHeartbeatAt) {
                        nextHeartbeatAt = now + TYPING_HEARTBEAT_MS
                        // A timeout has an unknown outcome; cleanup must still send false.
                        possiblyTypingChats.add(chatId)
                        broadcastTyping(chatId, true)
                        continue
                    }

                    if (chatId == null && pendingStops.isEmpty()) {
                        typingWakeups.receive()
                    } else {
                        withTimeoutOrNull(TYPING_STATE_POLL_MS) { typingWakeups.receive() }
                    }
                }
            } finally {
                // Bounded NonCancellable stop, using captured chat IDs because providers now point at the next chat.
                withContext(NonCancellable) {
                    withTimeoutOrNull(TYPING_CLEANUP_TIMEOUT_MS) {
                        val chatIds = (pendingStops.keys + possiblyTypingChats).distinct()
                        chatIds.forEach { chatId ->
                            var attempts = 0
                            while (attempts < STOP_ATTEMPTS) {
                                if (broadcastTyping(chatId, false)) break
                                attempts++
                                if (attempts < STOP_ATTEMPTS) delay(STOP_RETRY_MS)
                            }
                        }
                    }
                }
                typingChatId = null
                pendingStops.clear()
                possiblyTypingChats.clear()
                typingWakeups.close()
            }
        }
    }

    private fun canBroadcastTyping(): Boolean =
        screenVisible && isScreenVisibleProvider() && typingIndicatorsEnabledProvider()

    private suspend fun broadcastTyping(chatId: String, isTyping: Boolean): Boolean = try {
        withTimeoutOrNull(TYPING_NETWORK_TIMEOUT_MS) {
            broadcastTypingStatusUseCase(chatId, isTyping).getOrThrow()
            true
        } ?: false
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Failed to broadcast typing status", e, tag = "ChatInput")
        false
    }

    fun onInputTextChange(newText: String) {
        val changed = _inputText.value != newText
        _inputText.value = newText
        val chatId = currentChatIdProvider()
        if (newText.isBlank() || chatId.isNullOrBlank() || !canBroadcastTyping() || !viewModelScope.isActive) {
            stopTyping()
            return
        }
        // Restoring an unchanged draft (including on resume) is not new input activity.
        if (!changed) return
        if (typingChatId != chatId) {
            stopTyping()
            typingChatId = chatId
            nextHeartbeatAt = 0
        }
        lastInputActivityAt = SystemClock.elapsedRealtime()
        pendingStops.remove(chatId)
        typingWakeups.trySend(Unit)
    }

    fun stopTyping() {
        val chatId = typingChatId ?: return
        typingChatId = null
        pendingStops[chatId] = PendingStop()
        typingWakeups.trySend(Unit)
    }

    fun onVisibilityChanged(visible: Boolean) {
        screenVisible = visible
        if (!visible) stopTyping()
        // Becoming visible deliberately does not publish an unsent draft.
    }

    private companion object {
        const val TYPING_HEARTBEAT_MS = 2_000L
        const val TYPING_IDLE_MS = 3_000L
        const val TYPING_NETWORK_TIMEOUT_MS = 1_500L
        const val TYPING_CLEANUP_TIMEOUT_MS = 3_500L
        const val TYPING_STATE_POLL_MS = 250L
        const val STOP_RETRY_MS = 500L
        const val STOP_ATTEMPTS = 3
    }

    fun sendMessage() {
        stopTyping()
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

        // Consume this draft now so repeated taps cannot enqueue it twice.
        _replyingToMessage.value = null
        _inputText.value = ""

        // Past every "nothing to send" check, so this says the composer really did hand a message over —
        // regardless of how many chunks it was split into.
        onSendDispatched()

        sendChunks(chatId, chunks, expiresAt, replyToId)
    }

    // Runs inside the per-chat send lock, so message order is preserved while the
    // optimistic bubble is already visible. Once a wait times out for a chat, later
    // sends skip it instead of each adding the full timeout.
    private suspend fun awaitE2EEReady(chatId: String) {
        if (isE2EEReadyProvider() || e2eeWaitExhaustedChats.contains(chatId)) return
        Napier.d("E2EE not ready, waiting for initialization...", tag = "E2EE")
        var attempts = 0
        while (!isE2EEReadyProvider() && attempts < 30) {
            delay(100)
            attempts++
        }
        if (!isE2EEReadyProvider()) {
            e2eeWaitExhaustedChats.add(chatId)
            Napier.w("E2EE initialization timeout, proceeding anyway", tag = "E2EE")
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
                awaitReady = { awaitE2EEReady(chatId) },
                onError = { error ->
                    _error.value = "Failed to send: $error"
                    _toastMessage.value = error ?: "Failed to send message"
                }
            )
        }
    }

    fun startEditing(message: Message) {
        stopTyping()
        _editingMessage.value = message
        _inputText.value = message.content ?: ""
    }

    fun cancelEditing() {
        stopTyping()
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

    fun deleteSelectedMessagesForMe() {
        val selectedIds = _selectedMessageIds.value.toList()
        if (selectedIds.isEmpty()) return

        messagingDelegate.deleteSelectedMessagesForMe(
            selectedIds = selectedIds,
            onError = { _error.value = "Failed to delete messages: $it" },
            onSuccess = { _selectedMessageIds.value = emptySet() }
        )
    }

    fun deleteSelectedMessagesForEveryone() {
        val selectedIds = _selectedMessageIds.value.toList()
        if (selectedIds.isEmpty()) return

        messagingDelegate.deleteSelectedMessagesForEveryone(
            selectedIds = selectedIds,
            onError = { _error.value = "Failed to delete messages: $it" },
            onSuccess = { _selectedMessageIds.value = emptySet() }
        )
    }

    fun setReplyingToMessage(message: Message) {
        _replyingToMessage.value = message
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    fun cleanup() {
        stopTyping()
        // Keep the single scope-owned writer for the next chat; scope cancellation
        // performs the bounded final stop if this is the ViewModel's teardown.
    }
}
