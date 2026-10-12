package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsDeliveredUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SubscribeToTypingStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ChatSubscriptionDelegateTest {
    private fun message(senderId: String) = Message(
        id = "message-id",
        chatId = "chat-id",
        senderId = senderId,
        content = "hello",
        messageType = MessageType.TEXT,
        createdAt = "2026-10-08T10:00:00Z"
    )

    private suspend fun assertDeliveryCalls(message: Message, expectedCalls: Int) {
        val events = MutableSharedFlow<Message>()
        val subscribeToMessages = mock(SubscribeToMessagesUseCase::class.java)
        `when`(subscribeToMessages.invoke("chat-id")).thenReturn(events)

        val subscribeToTyping = mock(SubscribeToTypingStatusUseCase::class.java)
        `when`(subscribeToTyping.invoke("chat-id")).thenReturn(emptyFlow<TypingStatus>())
        val subscribeToReactions = mock(SubscribeToMessageReactionsUseCase::class.java)
        `when`(subscribeToReactions.invoke("chat-id")).thenReturn(emptyFlow<MessageReaction>())
        val markAsRead = mock(MarkMessagesAsReadUseCase::class.java)
        val markAsDelivered = mock(MarkMessagesAsDeliveredUseCase::class.java)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val delegate = ChatSubscriptionDelegate(
            subscribeToMessagesUseCase = subscribeToMessages,
            subscribeToTypingStatusUseCase = subscribeToTyping,
            subscribeToMessageReactionsUseCase = subscribeToReactions,
            markMessagesAsReadUseCase = markAsRead,
            markMessagesAsDeliveredUseCase = markAsDelivered,
            viewModelScope = scope,
            currentUserIdProvider = { "current-user" },
            isScreenVisibleProvider = { false },
            onNewMessage = {},
            onReactionEvent = {}
        )

        try {
            delegate.startSubscriptions("chat-id")
            events.emit(message)
            if (expectedCalls == 0) {
                verify(markAsDelivered, never()).invoke("chat-id")
            } else {
                verify(markAsDelivered, times(expectedCalls)).invoke("chat-id")
            }
        } finally {
            delegate.cleanup()
            scope.cancel()
        }
    }

    @Test
    fun senderEchoDoesNotMarkMessagesDelivered() = runBlocking {
        assertDeliveryCalls(message(senderId = "current-user"), expectedCalls = 0)
    }

    @Test
    fun incomingMessageMarksMessagesDelivered() = runBlocking {
        assertDeliveryCalls(message(senderId = "peer-user"), expectedCalls = 1)
    }
}
