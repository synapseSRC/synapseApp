package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMapperReplyTest {
    @Test
    fun messageRetrievalRetainsOriginalReplyReference() {
        val dto = MessageDto(
            id = "reply-id",
            chatId = "chat-id",
            senderId = "sender-id",
            content = "reply text",
            messageType = "text",
            replyToId = "original-message-id",
            createdAt = "2026-01-01T00:00:00Z"
        )

        val restored = ChatMapper.run { dto.toDomain() }

        assertEquals("original-message-id", restored.replyToId)
    }
}
return 