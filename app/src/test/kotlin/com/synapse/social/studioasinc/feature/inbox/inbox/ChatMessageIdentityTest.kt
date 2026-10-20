package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMessageIdentityTest {

    @Test
    fun optimisticMessageRetainsClientIdWhenReconciledWithServerConfirmedMessage() {
        val tempId = "temp-client-uuid-1234"
        val serverId = "server-confirmed-msg-9876"
        val createdAt = "2026-10-19T09:00:00Z"

        // 1. Optimistic message created locally
        val optimisticMessage = Message(
            id = tempId,
            chatId = "chat-1",
            senderId = "user-me",
            content = "Hello, world!",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENDING,
            createdAt = createdAt,
            sentAt = createdAt,
            clientId = tempId
        )

        assertEquals("temp-client-uuid-1234", optimisticMessage.clientId)
        assertEquals("temp-client-uuid-1234", optimisticMessage.id)

        // 2. Server confirmation arrives with server ID and SENT status
        val serverMessage = Message(
            id = serverId,
            chatId = "chat-1",
            senderId = "user-me",
            content = "Hello, world!",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENT,
            createdAt = createdAt,
            sentAt = createdAt,
            clientId = serverId // Server initially returns its own ID
        )

        // 3. Reconcile optimistic message with server message
        val reconciledMessage = optimisticMessage.mergeMonotonic(serverMessage.copy(clientId = optimisticMessage.clientId))

        // Persistent server ID is assigned, but local clientId MUST be preserved
        assertEquals(serverId, reconciledMessage.id)
        assertEquals(tempId, reconciledMessage.clientId)
        assertEquals(DeliveryStatus.SENT, reconciledMessage.deliveryStatus)
    }

    @Test
    fun chatItemsMapperPreservesOrderAndProducesStableMessageKeys() {
        val userMe = "user-me"
        val msg1 = Message(
            id = "msg-1",
            chatId = "chat-1",
            senderId = userMe,
            content = "First message",
            messageType = MessageType.TEXT,
            createdAt = "2026-10-19T08:00:00Z",
            clientId = "client-msg-1"
        )
        val msg2 = Message(
            id = "msg-2",
            chatId = "chat-1",
            senderId = "user-other",
            content = "Second message",
            messageType = MessageType.TEXT,
            createdAt = "2026-10-19T08:05:00Z",
            clientId = "client-msg-2"
        )

        val chatItems = ChatItemsMapper.buildChatItems(listOf(msg2, msg1), userMe)

        // Date divider should precede sorted messages
        assertTrue(chatItems.isNotEmpty())
        val firstItem = chatItems.first()
        assertTrue(firstItem is ChatListItem.DateDivider)

        val messageItems = chatItems.filterIsInstance<ChatListItem.MessageItem>()
        assertEquals(2, messageItems.size)
        assertEquals("client-msg-1", messageItems[0].message.clientId)
        assertEquals("client-msg-2", messageItems[1].message.clientId)
    }

    @Test
    fun deliveryStatusMonotonicallyAdvancesWithoutAffectingClientId() {
        val tempId = "client-uuid-abc"
        val initialMessage = Message(
            id = "server-msg-1",
            chatId = "chat-1",
            senderId = "user-me",
            content = "Important message",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENT,
            createdAt = "2026-10-19T08:00:00Z",
            clientId = tempId
        )

        val deliveredUpdate = initialMessage.copy(deliveryStatus = DeliveryStatus.DELIVERED)
        val mergedDelivered = initialMessage.mergeMonotonic(deliveredUpdate)

        assertEquals(DeliveryStatus.DELIVERED, mergedDelivered.deliveryStatus)
        assertEquals(tempId, mergedDelivered.clientId)

        val readUpdate = initialMessage.copy(deliveryStatus = DeliveryStatus.READ)
        val mergedRead = mergedDelivered.mergeMonotonic(readUpdate)

        assertEquals(DeliveryStatus.READ, mergedRead.deliveryStatus)
        assertEquals(tempId, mergedRead.clientId)
    }
}
