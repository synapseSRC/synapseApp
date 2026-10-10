package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.usecase.chat.BulkDeleteMessagesForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageForMeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.DeleteMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.EditMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.PopulateMessageReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.concurrent.CountDownLatch

class ChatMessagingDelegateTest {
    private fun message(id: String, createdAt: String) = Message(
        id = id,
        chatId = "chat-id",
        senderId = "sender-id",
        content = id,
        messageType = MessageType.TEXT,
        createdAt = createdAt
    )

    private fun delegate(scope: CoroutineScope) = ChatMessagingDelegate(
        sendMessageUseCase = mock(SendMessageUseCase::class.java),
        editMessageUseCase = mock(EditMessageUseCase::class.java),
        deleteMessageUseCase = mock(DeleteMessageUseCase::class.java),
        deleteMessageForMeUseCase = mock(DeleteMessageForMeUseCase::class.java),
        bulkDeleteMessagesForMeUseCase = mock(BulkDeleteMessagesForMeUseCase::class.java),
        populateMessageReactionsUseCase = mock(PopulateMessageReactionsUseCase::class.java),
        viewModelScope = scope,
        currentUserIdProvider = { "current-user" }
    )

    @Test
    fun quoteOnlyOlderMessageDoesNotMovePaginationCursor() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val messaging = delegate(scope)
            val loadedPage = listOf(
                message("page-oldest", "2026-10-08T10:00:00Z"),
                message("page-newest", "2026-10-09T10:00:00Z")
            )
            messaging.recordFetchedPage(loadedPage)
            messaging._messages.value = loadedPage

            messaging.addQuotedMessageToTimeline(message("quote-only-original", "2026-10-01T10:00:00Z"))

            assertEquals("page-oldest", messaging.oldestLoadedPageMessage?.id)
            assertEquals("quote-only-original", messaging.messages.value.first().id)

            messaging.recordFetchedPage(
                listOf(
                    message("next-page-oldest", "2026-10-06T10:00:00Z"),
                    message("next-page-newer", "2026-10-07T10:00:00Z")
                )
            )
            assertEquals("next-page-oldest", messaging.oldestLoadedPageMessage?.id)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun concurrentPageRecordsKeepTheEarliestCursor() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        try {
            val messaging = delegate(scope)
            val start = CountDownLatch(1)
            val workers = (0..20).map { minute ->
                Thread {
                    start.await()
                    repeat(100) {
                        val time = minute.toString().padStart(2, '0')
                        messaging.recordFetchedPage(listOf(message("page-$minute", "2026-10-08T10:$time:00Z")))
                    }
                }
            }

            workers.forEach { it.start() }
            start.countDown()
            workers.forEach { it.join() }

            assertEquals("page-0", messaging.oldestLoadedPageMessage?.id)
        } finally {
            scope.cancel()
        }
    }
}
