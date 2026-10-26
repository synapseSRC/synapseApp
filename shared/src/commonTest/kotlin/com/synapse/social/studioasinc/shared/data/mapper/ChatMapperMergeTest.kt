package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.mergeMonotonic
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatMapperMergeTest {
    private fun message(
        id: String,
        content: String,
        createdAt: String,
        clientId: String = id,
        encryptionFailureReason: String? = null,
        deliveryStatus: DeliveryStatus = DeliveryStatus.SENT
    ) = Message(
        id = id,
        chatId = "chat-1",
        senderId = "sender-1",
        content = content,
        messageType = MessageType.TEXT,
        deliveryStatus = deliveryStatus,
        createdAt = createdAt,
        clientId = clientId,
        encryptionFailureReason = encryptionFailureReason
    )

    @Test
    fun blankUndecryptableUpdateDoesNotReplaceReadableCachedContent() {
        val readable = message("message-1", "Readable cached content", "2026-01-01T10:00:00Z")
        val unreadable = readable.copy(
            content = "",
            encryptionFailureReason = "decryption_unavailable",
            deliveryStatus = DeliveryStatus.READ
        )

        val merged = readable.mergeMonotonic(unreadable)

        assertEquals("Readable cached content", merged.content)
        assertNull(merged.encryptionFailureReason)
        assertEquals(DeliveryStatus.READ, merged.deliveryStatus)
    }

    @Test
    fun mergeChronologicallyCollapsesDuplicateClientIdsAndSortsMessages() {
        val optimistic = message(
            "temporary-id", "Hello", "2026-01-01T10:00:00Z",
            clientId = "stable-client-id",
            deliveryStatus = DeliveryStatus.SENDING
        )
        val serverMessage = message(
            "server-id", "Hello", "2026-01-01T10:00:01Z",
            clientId = "stable-client-id",
            deliveryStatus = DeliveryStatus.SENT
        )
        val older = message("older", "Earlier", "2026-01-01T09:59:00Z")

        val merged = ChatMapper.mergeChronologically(
            current = listOf(optimistic, serverMessage),
            incoming = listOf(older, serverMessage)
        )

        assertEquals(listOf("older", "server-id"), merged.map { it.id })
        assertEquals("stable-client-id", merged.last().clientId)
    }

    @Test
    fun unreadableIncomingCopyDoesNotReplaceReadableContentDuringTimelineMerge() {
        val readable = message("message-2", "Previously decrypted", "2026-01-01T10:00:00Z")
        val unreadable = readable.copy(
            content = "{\"ciphertext\":\"redacted\"}",
            encryptionFailureReason = "decryption_unavailable"
        )

        val merged = ChatMapper.mergeChronologically(listOf(readable), listOf(unreadable))

        assertEquals(1, merged.size)
        assertEquals("Previously decrypted", merged.single().content)
        assertNull(merged.single().encryptionFailureReason)
    }
}
