package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToTypingStatusUseCase
import android.os.SystemClock
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    private val readReceiptsEnabledProvider: () -> Boolean = { true },
    private val onNewMessage: (Message) -> Unit,
    private val onReactionEvent: (MessageReaction) -> Unit,
    private val onSyncRequired: (suspend (String) -> Unit)? = null
) {

    private var messageSubscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null
    private var reactionSubscriptionJob: Job? = null
    private var typingTimeoutJob: Job? = null
    private var periodicSyncJob: Job? = null
    private var readySyncJob: Job? = null
    private var activeChatId: String? = null
    private var subscriptionToken = 0L
    private var typingStatusToken = 0L

    private data class ActiveTyping(val status: TypingStatus, val expiresAt: Long)
    private val activeTypingUsers = linkedMapOf<String, ActiveTyping>()

    val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus.asStateFlow()

    private enum class ReceiptLevel { DELIVERED, READ }

    private val receiptLock = Any()
    private var receiptToken = 0L
    private val pendingReceiptLevels = mutableMapOf<String, ReceiptLevel>()
    private val receiptFlushJobs = mutableMapOf<String, Job>()

    fun queueReadReceipt(chatId: String) {
        enqueueReceipt(chatId, ReceiptLevel.READ)
    }

    fun enqueueIncomingReceipt(chatId: String) {
        enqueueReceipt(chatId, if (isScreenVisibleProvider()) ReceiptLevel.READ else ReceiptLevel.DELIVERED)
    }

    private fun mergeReceipt(chatId: String, requested: ReceiptLevel) {
        if (pendingReceiptLevels[chatId] != ReceiptLevel.READ) {
            pendingReceiptLevels[chatId] = requested
        }
    }

    private fun enqueueReceipt(chatId: String, requested: ReceiptLevel) {
        synchronized(receiptLock) {
            val activeId = activeChatId ?: return
            if (!activeId.equals(chatId, ignoreCase = true) || !viewModelScope.isActive) return
            mergeReceipt(activeId, requested)
            if (receiptFlushJobs[activeId]?.isActive != true) {
                val token = receiptToken
                val job = viewModelScope.launch(start = CoroutineStart.LAZY) { flushReceiptBatch(activeId, token) }
                receiptFlushJobs[activeId] = job
                job.start()
            }
        }
    }

    private fun isReceiptCurrent(chatId: String, token: Long): Boolean = synchronized(receiptLock) {
        token == receiptToken && activeChatId.equals(chatId, ignoreCase = true)
    }

    private suspend fun flushReceiptBatch(chatId: String, token: Long) {
        val job = currentCoroutineContext()[Job]
        var retryDelay = RECEIPT_RETRY_INITIAL_MS
        var waitMs = RECEIPT_BATCH_WINDOW_MS
        try {
            while (currentCoroutineContext().isActive && isReceiptCurrent(chatId, token)) {
                delay(waitMs)
                val level = synchronized(receiptLock) {
                    if (!isReceiptCurrent(chatId, token)) return
                    val pending = pendingReceiptLevels.remove(chatId)
                    if (pending == null && receiptFlushJobs[chatId] === job) receiptFlushJobs.remove(chatId)
                    pending
                } ?: return

                val succeeded = try {
                    withTimeoutOrNull(RECEIPT_NETWORK_TIMEOUT_MS) {
                        currentCoroutineContext().ensureActive()
                        if (!isReceiptCurrent(chatId, token)) return@withTimeoutOrNull false
                        // Current visibility decides whether a read acknowledgement is allowed, never a queued level.
                        val result = if (isScreenVisibleProvider()) {
                            markMessagesAsReadUseCase(chatId, shareReceipt = readReceiptsEnabledProvider())
                        } else {
                            markMessagesAsDeliveredUseCase(chatId)
                        }
                        result.getOrThrow()
                        true
                    } ?: false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.w("Failed to update message receipts", e, tag = "ChatSubscription")
                    false
                }

                currentCoroutineContext().ensureActive()
                if (!isReceiptCurrent(chatId, token)) return
                if (succeeded) {
                    retryDelay = RECEIPT_RETRY_INITIAL_MS
                    waitMs = RECEIPT_BATCH_WINDOW_MS
                } else {
                    synchronized(receiptLock) {
                        if (isReceiptCurrent(chatId, token)) mergeReceipt(chatId, level)
                    }
                    waitMs = retryDelay
                    retryDelay = (retryDelay * 2).coerceAtMost(RECEIPT_RETRY_MAX_MS)
                }
            }
        } finally {
            synchronized(receiptLock) {
                if (receiptFlushJobs[chatId] === job) receiptFlushJobs.remove(chatId)
            }
        }
    }

    private companion object {
        const val RECEIPT_BATCH_WINDOW_MS = 200L
        const val RECEIPT_NETWORK_TIMEOUT_MS = 5_000L
        const val RECEIPT_RETRY_INITIAL_MS = 500L
        const val RECEIPT_RETRY_MAX_MS = 5_000L
        const val TYPING_EXPIRY_MS = 5_000L
        const val TYPING_EXPIRY_POLL_MS = 250L
        const val SYNC_TIMEOUT_MS = 10_000L
        const val INCREMENTAL_RECOVERY_INTERVAL_MS = 60_000L
    }

    fun restartSubscriptions(chatId: String) {
        startSubscriptions(chatId)
    }

    private fun isSubscriptionCurrent(chatId: String, token: Long): Boolean =
        token == subscriptionToken && activeChatId.equals(chatId, ignoreCase = true)

    // Readiness comes only from the realtime connect callback; Flow<Message> carries no readiness signal.
    fun onSubscriptionReady(chatId: String) {
        val token = subscriptionToken
        if (!isSubscriptionCurrent(chatId, token) || !viewModelScope.isActive) return
        enqueueIncomingReceipt(chatId)
        if (readySyncJob?.isActive == true) return
        readySyncJob = viewModelScope.launch { syncMessages(chatId, token) }
    }

    private suspend fun syncMessages(chatId: String, token: Long) {
        if (!isSubscriptionCurrent(chatId, token)) return
        try {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) { onSyncRequired?.invoke(chatId) }
            if (isSubscriptionCurrent(chatId, token)) enqueueIncomingReceipt(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Incremental recovery sync failed", e, tag = "ChatSubscription")
        }
    }

    fun startSubscriptions(chatId: String) {
        if (activeChatId.equals(chatId, ignoreCase = true)) {
            stopSubscriptionJobs() // Same-chat reconnect retains pending receipts and retries.
        } else {
            cleanup()
        }
        synchronized(receiptLock) { activeChatId = chatId }
        val token = subscriptionToken
        messageSubscriptionJob = viewModelScope.launch {
            while (isActive && isSubscriptionCurrent(chatId, token)) {
                try {
                    subscribeToMessagesUseCase.onConnected(chatId) {
                        // Drop to the ViewModel dispatcher but keep this subscription's Job as the parent.
                        withContext(viewModelScope.coroutineContext.minusKey(Job)) {
                            currentCoroutineContext().ensureActive()
                            if (isSubscriptionCurrent(chatId, token)) onSubscriptionReady(chatId)
                        }
                    }.collect { newMessage ->
                        currentCoroutineContext().ensureActive()
                        if (!isSubscriptionCurrent(chatId, token) || !newMessage.chatId.equals(chatId, ignoreCase = true)) return@collect
                        try {
                            dismissTypingForUser(newMessage.senderId)
                            onNewMessage(newMessage)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            Napier.e("Error processing new realtime message", e)
                        }

                        // Judge from the server event, not the optimistic copy; echoes already containing us need no write.
                        val currentUserId = currentUserIdProvider()
                        if (isSubscriptionCurrent(chatId, token) && !currentUserId.isNullOrBlank() &&
                            newMessage.senderId.isNotBlank() && newMessage.senderId != currentUserId &&
                            currentUserId !in newMessage.readBy &&
                            (isScreenVisibleProvider() || newMessage.deliveryStatus !in setOf(
                                com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.DELIVERED,
                                com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.READ
                            ))
                        ) {
                            enqueueIncomingReceipt(chatId)
                        }
                    }
                    if (isSubscriptionCurrent(chatId, token)) clearTypingStatus()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (isSubscriptionCurrent(chatId, token)) clearTypingStatus()
                    Napier.w("Messages subscription failed or ended for chat $chatId, reconnecting in 2s...", e, tag = "ChatSubscription")
                    delay(2000)
                }
                delay(100)
            }
        }

        typingSubscriptionJob = viewModelScope.launch {
            while (isActive && isSubscriptionCurrent(chatId, token)) {
                clearTypingStatus()
                try {
                    if (!typingIndicatorsEnabledProvider()) {
                        delay(1000)
                        continue
                    }
                    subscribeToTypingStatusUseCase(chatId).collect { status ->
                        currentCoroutineContext().ensureActive()
                        if (!isSubscriptionCurrent(chatId, token)) return@collect
                        handleTypingStatus(chatId, token, status)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (isSubscriptionCurrent(chatId, token)) clearTypingStatus()
                    delay(2000)
                } finally {
                    if (isSubscriptionCurrent(chatId, token)) clearTypingStatus()
                }
                delay(100)
            }
        }

        reactionSubscriptionJob = viewModelScope.launch {
            while (isActive && isSubscriptionCurrent(chatId, token)) {
                try {
                    subscribeToMessageReactionsUseCase(chatId).collect { reaction ->
                        currentCoroutineContext().ensureActive()
                        if (isSubscriptionCurrent(chatId, token)) onReactionEvent(reaction)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    delay(2000)
                }
                delay(100) // Reduced from 1 s — trims reconnect dead window
            }
        }

        periodicSyncJob = viewModelScope.launch {
            while (isActive && isSubscriptionCurrent(chatId, token)) {
                delay(INCREMENTAL_RECOVERY_INTERVAL_MS)
                if (isScreenVisibleProvider()) syncMessages(chatId, token)
            }
        }
    }

    private fun handleTypingStatus(chatId: String, token: Long, status: TypingStatus) {
        val myUserId = currentUserIdProvider()
        if (!isScreenVisibleProvider() || !typingIndicatorsEnabledProvider() || myUserId.isNullOrBlank()) {
            clearTypingStatus()
            return
        }
        if (!status.chatId.equals(chatId, ignoreCase = true) || status.userId.isBlank() || status.userId == myUserId) return

        activeTypingUsers.remove(status.userId)
        if (status.isTyping) {
            activeTypingUsers[status.userId] = ActiveTyping(status, SystemClock.elapsedRealtime() + TYPING_EXPIRY_MS)
        }
        publishTypingStatus()
        if (activeTypingUsers.isEmpty()) {
            clearTypingStatus()
        } else if (typingTimeoutJob?.isActive != true) {
            val expiryToken = typingStatusToken
            val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
                try {
                    while (isActive && isSubscriptionCurrent(chatId, token) && expiryToken == typingStatusToken) {
                        delay(TYPING_EXPIRY_POLL_MS)
                        if (!isScreenVisibleProvider() || !typingIndicatorsEnabledProvider()) {
                            clearTypingStatus()
                            return@launch
                        }
                        publishTypingStatus()
                        if (activeTypingUsers.isEmpty()) return@launch
                    }
                } finally {
                    if (expiryToken == typingStatusToken) typingTimeoutJob = null
                }
            }
            typingTimeoutJob = job
            job.start()
        }
    }

    fun dismissTypingForUser(userId: String) {
        if (userId.isBlank()) return
        if (activeTypingUsers.remove(userId) != null) {
            publishTypingStatus()
            if (activeTypingUsers.isEmpty()) {
                clearTypingStatus()
            }
        }
    }

    private fun publishTypingStatus() {
        val now = SystemClock.elapsedRealtime()
        val myUserId = currentUserIdProvider()
        activeTypingUsers.entries.removeAll {
            it.value.expiresAt <= now || myUserId.isNullOrBlank() || it.key == myUserId
        }
        // Keep the single-user UI contract; a member's false falls back to another still-active member.
        _typingStatus.value = activeTypingUsers.values.lastOrNull()?.status
    }

    fun clearTypingStatus() {
        typingStatusToken++
        typingTimeoutJob?.cancel()
        typingTimeoutJob = null
        activeTypingUsers.clear()
        _typingStatus.value = null
    }

    private fun stopSubscriptionJobs() {
        subscriptionToken++
        messageSubscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
        reactionSubscriptionJob?.cancel()
        periodicSyncJob?.cancel()
        readySyncJob?.cancel()
        messageSubscriptionJob = null
        typingSubscriptionJob = null
        reactionSubscriptionJob = null
        periodicSyncJob = null
        readySyncJob = null
        clearTypingStatus()
    }

    fun cleanup() {
        stopSubscriptionJobs()
        synchronized(receiptLock) {
            activeChatId = null
            receiptToken++
            receiptFlushJobs.values.forEach { it.cancel() }
            receiptFlushJobs.clear()
            pendingReceiptLevels.clear()
        }
        // Receiver lifetime/ref-counting owns channel release in the shared data layer.
    }
}
