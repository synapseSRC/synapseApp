package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToTypingStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatSubscriptionDelegate(
    private val subscribeToMessagesUseCase: SubscribeToMessagesUseCase,
    private val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase,
    private val subscribeToMessageReactionsUseCase: SubscribeToMessageReactionsUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val isScreenVisibleProvider: () -> Boolean,
    private val typingIndicatorsEnabledProvider: () -> Boolean = { true },
    private val onNewMessage: (Message) -> Unit,
    private val onReactionEvent: (MessageReaction) -> Unit,
    private val onSyncRequired: (suspend () -> Unit)? = null,
    private val onCleanupTypingChannel: (suspend () -> Unit)? = null
) {

    private var messageSubscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null
    private var reactionSubscriptionJob: Job? = null
    private var typingTimeoutJob: Job? = null
    private var periodicSyncJob: Job? = null

    val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus.asStateFlow()

    private enum class ReceiptLevel { DELIVERED, READ }

    private val receiptLock = Any()
    private val pendingReceiptLevels = mutableMapOf<String, ReceiptLevel>()
    private val receiptFlushJobs = mutableMapOf<String, Job>()

    fun queueReadReceipt(chatId: String) {
        enqueueReceipt(chatId, ReceiptLevel.READ)
    }

    private fun enqueueIncomingReceipt(chatId: String) {
        enqueueReceipt(chatId, if (isScreenVisibleProvider()) ReceiptLevel.READ else ReceiptLevel.DELIVERED)
    }

    private fun enqueueReceipt(chatId: String, requested: ReceiptLevel) {
        synchronized(receiptLock) {
            val current = pendingReceiptLevels[chatId]
            if (current != ReceiptLevel.READ) {
                pendingReceiptLevels[chatId] = if (requested == ReceiptLevel.READ) ReceiptLevel.READ else current ?: requested
            }
            if (receiptFlushJobs[chatId]?.isActive != true) {
                receiptFlushJobs[chatId] = viewModelScope.launch { flushReceiptBatch(chatId) }
            }
        }
    }

    private suspend fun flushReceiptBatch(chatId: String) {
        while (true) {
            delay(RECEIPT_BATCH_WINDOW_MS)
            val level = synchronized(receiptLock) {
                val pending = pendingReceiptLevels.remove(chatId)
                if (pending == null) receiptFlushJobs.remove(chatId)
                pending
            } ?: return

            try {
                // READ wins if messages crossed the coalescing window with mixed visibility.
                if (level == ReceiptLevel.READ || isScreenVisibleProvider()) {
                    markMessagesAsReadUseCase(chatId).onFailure {
                        io.github.aakira.napier.Napier.e("Failed to mark messages as read", it)
                    }
                } else {
                    markMessagesAsDeliveredUseCase(chatId).onFailure {
                        io.github.aakira.napier.Napier.e("Failed to mark messages as delivered", it)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                io.github.aakira.napier.Napier.e("Failed to update message receipts", e)
            }
        }
    }

    private companion object {
        const val RECEIPT_BATCH_WINDOW_MS = 200L
        const val INCREMENTAL_RECOVERY_INTERVAL_MS = 60_000L
    }

    fun restartSubscriptions(chatId: String) {
        cleanup()
        startSubscriptions(chatId)
    }

    fun startSubscriptions(chatId: String) {
        cleanup()
        messageSubscriptionJob = viewModelScope.launch {
            var isInitial = true
            while (isActive) {
                try {
                    if (!isInitial) {
                        io.github.aakira.napier.Napier.d("Subscription reconnected for chat $chatId, syncing missed messages", tag = "ChatSubscription")
                        try {
                            onSyncRequired?.invoke()
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            io.github.aakira.napier.Napier.w("Error syncing missed messages on reconnect", e, tag = "ChatSubscription")
                        }
                    }
                    isInitial = false
                    subscribeToMessagesUseCase(chatId).collect { newMessage ->
                        try {
                            onNewMessage(newMessage)
                        } catch (e: Exception) {
                            io.github.aakira.napier.Napier.e("Error processing new realtime message", e)
                        }

                        // Only mark as read/delivered if the message is from someone else.
                        // We also check if the screen is currently visible to the user.
                        val currentUserId = currentUserIdProvider()
                        if (currentUserId != null && newMessage.senderId != currentUserId) {
                            enqueueIncomingReceipt(chatId)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    io.github.aakira.napier.Napier.w("Messages subscription failed or ended for chat $chatId, reconnecting in 2s...", e, tag = "ChatSubscription")
                    delay(2000)
                }
                delay(100) // Reduced from 1 s — trims reconnect dead window
            }
        }

        typingSubscriptionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    if (!typingIndicatorsEnabledProvider()) {
                        delay(1000)
                        continue
                    }
                    subscribeToTypingStatusUseCase(chatId).collect { status ->
                        val myUserId = currentUserIdProvider()
                        if (status.userId.isNotBlank() && (myUserId == null || status.userId != myUserId)) {
                            _typingStatus.value = if (status.isTyping) status else null

                            if (status.isTyping) {
                                typingTimeoutJob?.cancel()
                                typingTimeoutJob = viewModelScope.launch {
                                    delay(4000)
                                    _typingStatus.value = null
                                }
                            } else {
                                typingTimeoutJob?.cancel()
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    delay(2000)
                }
                delay(100) // Reduced from 1 s — trims reconnect dead window
            }
        }

        reactionSubscriptionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    subscribeToMessageReactionsUseCase(chatId).collect { reaction ->
                        onReactionEvent(reaction)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    delay(2000)
                }
                delay(100) // Reduced from 1 s — trims reconnect dead window
            }
        }

        periodicSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(INCREMENTAL_RECOVERY_INTERVAL_MS)
                if (isScreenVisibleProvider()) {
                    try {
                        onSyncRequired?.invoke()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        io.github.aakira.napier.Napier.w("Incremental recovery sync failed", e, tag = "ChatSubscription")
                    }
                }
            }
        }
    }

    fun cleanup() {
        messageSubscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
        reactionSubscriptionJob?.cancel()
        typingTimeoutJob?.cancel()
        periodicSyncJob?.cancel()
        messageSubscriptionJob = null
        typingSubscriptionJob = null
        reactionSubscriptionJob = null
        typingTimeoutJob = null
        periodicSyncJob = null
        _typingStatus.value = null
        // Release the persistent typing broadcast channel to avoid leaking WebSocket channels
        onCleanupTypingChannel?.let { cleanupFn ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try { cleanupFn() } catch (_: Exception) {}
            }
        }
    }
}
