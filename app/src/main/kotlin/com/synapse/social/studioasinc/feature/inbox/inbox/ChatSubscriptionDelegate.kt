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
import kotlinx.coroutines.launch

class ChatSubscriptionDelegate(
    private val subscribeToMessagesUseCase: SubscribeToMessagesUseCase,
    private val subscribeToTypingStatusUseCase: SubscribeToTypingStatusUseCase,
    private val subscribeToMessageReactionsUseCase: SubscribeToMessageReactionsUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val markMessagesAsDeliveredUseCase: MarkMessagesAsDeliveredUseCase,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val onNewMessage: (Message) -> Unit,
    private val onReactionEvent: (MessageReaction) -> Unit
) {

    private var messageSubscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null
    private var reactionSubscriptionJob: Job? = null

    val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus.asStateFlow()

    fun startSubscriptions(chatId: String) {
        messageSubscriptionJob = viewModelScope.launch {
            subscribeToMessagesUseCase(chatId).collect { newMessage ->
                onNewMessage(newMessage)
                markMessagesAsReadUseCase(chatId)
                markMessagesAsDeliveredUseCase(chatId)
            }
        }

        typingSubscriptionJob = viewModelScope.launch {
            subscribeToTypingStatusUseCase(chatId).collect { status ->
                if (status.userId != currentUserIdProvider()) {
                    _typingStatus.value = if (status.isTyping) status else null
                }
            }
        }

        reactionSubscriptionJob = viewModelScope.launch {
            subscribeToMessageReactionsUseCase().collect { reaction ->
                onReactionEvent(reaction)
            }
        }
    }

    fun cleanup() {
        messageSubscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
        reactionSubscriptionJob?.cancel()
        messageSubscriptionJob = null
        typingSubscriptionJob = null
        reactionSubscriptionJob = null
    }
}
