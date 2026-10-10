package com.synapse.social.studioasinc.shared.domain.model.chat

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageLifecycleTest {

    @Test
    fun `test DTO to Domain mapping for SENDING state`() {
        val dto = MessageDto(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            deliveryStatus = "sending",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("msg1", domain.id)
        assertEquals(DeliveryStatus.SENDING, domain.deliveryStatus)
        assertEquals(ContentStatus.ACTIVE, domain.contentStatus)
        assertEquals("2026-10-06T10:00:00Z", domain.sentAt)
        assertNull(domain.deliveredAt)
        assertNull(domain.readAt)
    }

    @Test
    fun `test DTO to Domain mapping for SENT state`() {
        val dto = MessageDto(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            deliveryStatus = "sent",
            sentAt = "2026-10-06T10:00:00Z",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(DeliveryStatus.SENT, domain.deliveryStatus)
        assertEquals("2026-10-06T10:00:00Z", domain.sentAt)
    }

    @Test
    fun `test DTO to Domain mapping for DELIVERED state`() {
        val dto = MessageDto(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            deliveryStatus = "delivered",
            sentAt = "2026-10-06T10:00:00Z",
            deliveredAt = "2026-10-06T10:01:00Z",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(DeliveryStatus.DELIVERED, domain.deliveryStatus)
        assertEquals("2026-10-06T10:01:00Z", domain.deliveredAt)
    }

    @Test
    fun `test DTO to Domain mapping for READ state`() {
        val dto = MessageDto(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            deliveryStatus = "read",
            sentAt = "2026-10-06T10:00:00Z",
            deliveredAt = "2026-10-06T10:01:00Z",
            readAt = "2026-10-06T10:02:00Z",
            readBy = listOf("user2"),
            createdAt = "2026-10-06T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(DeliveryStatus.READ, domain.deliveryStatus)
        assertEquals("2026-10-06T10:02:00Z", domain.readAt)
        assertEquals(listOf("user2"), domain.readBy)
    }

    @Test
    fun `test DTO to Domain mapping for FAILED state`() {
        val dto = MessageDto(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            deliveryStatus = "failed",
            failureReason = "Network error",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(DeliveryStatus.FAILED, domain.deliveryStatus)
        assertEquals("Network error", domain.failureReason)
    }

    @Test
    fun `test monotonic status merge prevents regression from READ to SENT or DELIVERED`() {
        val current = Message(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.READ,
            contentStatus = ContentStatus.ACTIVE,
            sentAt = "2026-10-06T10:00:00Z",
            deliveredAt = "2026-10-06T10:01:00Z",
            readAt = "2026-10-06T10:02:00Z",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val staleUpdate = Message(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENT,
            contentStatus = ContentStatus.ACTIVE,
            createdAt = "2026-10-06T10:00:00Z"
        )

        val merged = current.mergeMonotonic(staleUpdate)

        assertEquals(DeliveryStatus.READ, merged.deliveryStatus)
        assertEquals("2026-10-06T10:02:00Z", merged.readAt)
        assertEquals("2026-10-06T10:01:00Z", merged.deliveredAt)
    }

    @Test
    fun `test FAILED status is not overwritten by stale success update`() {
        val failed = Message(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.FAILED,
            failureReason = "Network error",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val staleSuccess = Message(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENT,
            createdAt = "2026-10-06T10:00:00Z"
        )

        val merged = failed.mergeMonotonic(staleSuccess)

        assertEquals(DeliveryStatus.FAILED, merged.deliveryStatus)
        assertEquals("Network error", merged.failureReason)
    }

    @Test
    fun `test EDITED content status preserves deliveredAt and readAt timestamps`() {
        val original = Message(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Original text",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.READ,
            contentStatus = ContentStatus.ACTIVE,
            sentAt = "2026-10-06T10:00:00Z",
            deliveredAt = "2026-10-06T10:01:00Z",
            readAt = "2026-10-06T10:02:00Z",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val edited = Message(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Edited text",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.READ,
            contentStatus = ContentStatus.EDITED,
            isEdited = true,
            editedAt = "2026-10-06T10:05:00Z",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val merged = original.mergeMonotonic(edited)

        assertEquals("Edited text", merged.content)
        assertEquals(ContentStatus.EDITED, merged.contentStatus)
        assertTrue(merged.isEdited)
        assertEquals("2026-10-06T10:05:00Z", merged.editedAt)
        assertEquals("2026-10-06T10:01:00Z", merged.deliveredAt)
        assertEquals("2026-10-06T10:02:00Z", merged.readAt)
        assertEquals(DeliveryStatus.READ, merged.deliveryStatus)
    }

    @Test
    fun `test DELETED content status mapping`() {
        val dto = MessageDto(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "This message was deleted",
            contentState = "deleted",
            isDeleted = true,
            deletedAt = "2026-10-06T10:10:00Z",
            createdAt = "2026-10-06T10:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(ContentStatus.DELETED, domain.contentStatus)
        assertTrue(domain.isDeleted)
        assertEquals("2026-10-06T10:10:00Z", domain.deletedAt)
    }
}
