package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.PollMetadata
import com.synapse.social.studioasinc.shared.domain.model.chat.PollOption
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChatMapperTest {

    @Test
    fun testRemoteMessageTypeMappingAndMetadataParsing() {
        val pollMetadata = PollMetadata(
            pollId = "poll_123",
            question = "Favorite Language?",
            options = listOf(PollOption("1", "Kotlin"), PollOption("2", "Swift"))
        )
        val container = MessageMetadataContainer(poll = pollMetadata)
        val jsonString = Json.encodeToString(MessageMetadataContainer.serializer(), container)

        val dto = MessageDto(
            id = "msg_1",
            chatId = "chat_1",
            senderId = "user_1",
            content = "Poll Message",
            messageType = "poll",
            metadata = jsonString
        )

        val domain = dto.toDomain()

        assertEquals(MessageType.POLL, domain.messageType)
        assertNotNull(domain.metadataContainer)
        assertNotNull(domain.metadataContainer?.poll)
        assertEquals("Favorite Language?", domain.metadataContainer?.poll?.question)
    }

    @Test
    fun testAll26RemoteMessageTypesMapping() {
        val typesMap = mapOf(
            "text" to MessageType.TEXT,
            "voice" to MessageType.VOICE,
            "audio" to MessageType.AUDIO,
            "image" to MessageType.IMAGE,
            "video" to MessageType.VIDEO,
            "file" to MessageType.FILE,
            "contact" to MessageType.CONTACT,
            "location" to MessageType.LOCATION,
            "live_location" to MessageType.LIVE_LOCATION,
            "gif" to MessageType.GIF,
            "sticker" to MessageType.STICKER,
            "link_preview" to MessageType.LINK_PREVIEW,
            "poll" to MessageType.POLL,
            "music" to MessageType.MUSIC,
            "shared_post" to MessageType.SHARED_POST,
            "story_share" to MessageType.STORY_SHARE,
            "event" to MessageType.EVENT,
            "product" to MessageType.PRODUCT,
            "payment" to MessageType.PAYMENT,
            "map" to MessageType.MAP,
            "code_snippet" to MessageType.CODE_SNIPPET,
            "call" to MessageType.CALL,
            "system" to MessageType.SYSTEM,
            "ephemeral_media" to MessageType.EPHEMERAL_MEDIA
        )

        typesMap.forEach { (remoteString, expectedType) ->
            val dto = MessageDto(id = "msg", chatId = "chat", senderId = "user", content = "test", messageType = remoteString)
            assertEquals(expectedType, dto.toDomain().messageType)
        }
    }
}
