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
    private val onNewMessage: (Message) -> Unit,
    private val onReactionEvent: (MessageReaction) -> Unit
) {

    private var messageSubscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null
    private var reactionSubscriptionJob: Job? = null
    private var typingTimeoutJob: Job? = null

    val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus.asStateFlow()

    fun restartSubscriptions(chatId: String) {
        cleanup()
        startSubscriptions(chatId)
    }

    fun startSubscriptions(chatId: String) {
        messageSubscriptionJob = viewModelScope.launch {
            while (isActive) {
                try {
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
                            viewModelScope.launch {
                                try {
                                    if (isScreenVisibleProvider()) {
                                        markMessagesAsReadUseCase(chatId)
                                    } else {
                                        markMessagesAsDeliveredUseCase(chatId)
                                    }
                                } catch (e: Exception) {
                                    io.github.aakira.napier.Napier.e("Failed to mark messages as read/delivered", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    io.github.aakira.napier.Napier.w("Messages subscription failed or ended for chat $chatId, reconnecting in 2s...", e, tag = "ChatSubscription")
                    delay(2000)
                }
                delay(1000)
            }
        }

        typingSubscriptionJob = viewModelScope.launch {
            while (isActive) {
                try {
                    subscribeToTypingStatusUseCase(chatId).collect { status ->
                        if (status.userId != currentUserIdProvider()) {
                            _typingStatus.value = if (status.isTyping) status else null

                            if (status.isTyping) {
                                typingTimeoutJob?.cancel()
                                typingTimeoutJob = viewModelScope.launch {
                                    delay(3000)
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
                delay(1000)
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
                delay(1000)
            }
        }
    }

    fun cleanup() {
        messageSubscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
        reactionSubscriptionJob?.cancel()
        typingTimeoutJob?.cancel()
        messageSubscriptionJob = null
        typingSubscriptionJob = null
        reactionSubscriptionJob = null
        typingTimeoutJob = null
        _typingStatus.value = null
    }
}
