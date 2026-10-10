package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BroadcastTypingStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class ChatInputDelegateTest {
    @Test
    fun textReplyKeepsOriginalReferenceForSupportedTargetTypes() = runTest {
        val supportedTargets = MessageType.values().toList()

        supportedTargets.forEachIndexed { index, type ->
            val messaging = mock(ChatMessagingDelegate::class.java)
            doReturn(listOf("reply text")).`when`(messaging).splitIntoChunks("reply text", 500)
            val replyingTo = MutableStateFlow<Message?>(
                Message(
                    id = "target-$index",
                    chatId = "chat-id",
                    senderId = "other-user",
                    content = "target content",
                    messageType = type,
                    createdAt = "2026-01-01T00:00:00Z"
                )
            )
            val delegate = ChatInputDelegate(
                broadcastTypingStatusUseCase = mock(BroadcastTypingStatusUseCase::class.java),
                viewModelScope = CoroutineScope(StandardTestDispatcher(testScheduler)),
                messagingDelegate = messaging,
                _inputText = MutableStateFlow("reply text"),
                _editingMessage = MutableStateFlow(null),
                _error = MutableStateFlow(null),
                _replyingToMessage = replyingTo,
                _toastMessage = MutableStateFlow(null),
                _selectedMessageIds = MutableStateFlow(emptySet()),
                chatMaxMessageChunkSizeProvider = { 500 },
                currentChatIdProvider = { "chat-id" },
                isE2EEReadyProvider = { true },
                disappearingModeProvider = { DisappearingMode.OFF },
                onChatRefreshRequired = {}
            )

            delegate.sendMessage()

            val sendInvocations = mockingDetails(messaging).invocations
                .filter { it.method.name == "performSendMessage" }
            assertEquals("one send for $type", 1, sendInvocations.size)
            val sendArguments = sendInvocations.single().arguments
            assertEquals("chat id for $type", "chat-id", sendArguments[0])
            assertEquals("reply text for $type", "reply text", sendArguments[1])
            assertNull("expiry for $type", sendArguments[2])
            assertEquals("reply reference for $type", "target-$index", sendArguments[3])
            assertNotNull("error handler for $type", sendArguments[4])
            assertNull("reply state for $type", replyingTo.value)
        }
    }
}
