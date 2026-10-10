package com.synapse.social.studioasinc.feature.inbox.inbox.components

import com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.PollMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotedMessagePreviewModelTest {
    private fun message(
        type: MessageType,
        content: String = "",
        mediaUrl: String? = null,
        attachments: List<MessageAttachment> = emptyList(),
        metadata: MessageMetadataContainer? = null,
        deleted: Boolean = false
    ) = Message(
        id = "message-id",
        chatId = "chat-id",
        senderId = "sender-id",
        content = content,
        messageType = type,
        mediaUrl = mediaUrl,
        attachments = attachments,
        metadataContainer = metadata,
        isDeleted = deleted,
        contentStatus = if (deleted) ContentStatus.DELETED else ContentStatus.ACTIVE,
        createdAt = "2026-01-01T00:00:00Z"
    )

    @Test
    fun supportedMessageTypesMapToExplicitKinds() {
        val expected = mapOf(
            MessageType.TEXT to QuotedMessageKind.TEXT,
            MessageType.VOICE to QuotedMessageKind.VOICE,
            MessageType.AUDIO to QuotedMessageKind.AUDIO,
            MessageType.IMAGE to QuotedMessageKind.PHOTO,
            MessageType.VIDEO to QuotedMessageKind.VIDEO,
            MessageType.MEDIA_GROUP to QuotedMessageKind.MEDIA,
            MessageType.FILE to QuotedMessageKind.FILE,
            MessageType.CONTACT to QuotedMessageKind.CONTACT,
            MessageType.LOCATION to QuotedMessageKind.LOCATION,
            MessageType.LIVE_LOCATION to QuotedMessageKind.LIVE_LOCATION,
            MessageType.GIF to QuotedMessageKind.GIF,
            MessageType.STICKER to QuotedMessageKind.STICKER,
            MessageType.LINK_PREVIEW to QuotedMessageKind.LINK,
            MessageType.POLL to QuotedMessageKind.POLL,
            MessageType.MUSIC to QuotedMessageKind.MUSIC,
            MessageType.SHARED_POST to QuotedMessageKind.SHARED_POST,
            MessageType.STORY_SHARE to QuotedMessageKind.STORY,
            MessageType.EVENT to QuotedMessageKind.EVENT,
            MessageType.PRODUCT to QuotedMessageKind.PRODUCT,
            MessageType.PAYMENT to QuotedMessageKind.PAYMENT,
            MessageType.MAP to QuotedMessageKind.MAP,
            MessageType.CODE_SNIPPET to QuotedMessageKind.CODE,
            MessageType.CALL to QuotedMessageKind.CALL,
            MessageType.SYSTEM to QuotedMessageKind.SYSTEM,
            MessageType.EPHEMERAL_MEDIA to QuotedMessageKind.VIEW_ONCE
        )

        assertEquals(MessageType.values().toSet(), expected.keys)
        expected.forEach { (type, kind) ->
            assertEquals(type.name, kind, quotedMessagePreviewModel(message(type)).kind)
        }
    }

    @Test
    fun photoThumbnailAndVideoDurationUseMessageTypeNotUrlPresence() {
        val photo = quotedMessagePreviewModel(message(MessageType.IMAGE, content = "Sunset", mediaUrl = "https://cdn.example/photo.jpg"))
        assertEquals(QuotedMessageKind.PHOTO, photo.kind)
        assertEquals("https://cdn.example/photo.jpg", photo.thumbnailUrl)
        assertEquals("Sunset", photo.excerpt)

        val video = quotedMessagePreviewModel(
            message(
                MessageType.VIDEO,
                content = "Launch clip",
                mediaUrl = "https://cdn.example/video.mp4",
                attachments = listOf(MessageAttachment("https://cdn.example/video.mp4", AttachmentType.VIDEO, duration = 83))
            )
        )
        assertEquals(QuotedMessageKind.VIDEO, video.kind)
        assertNull(video.thumbnailUrl)
        assertEquals(83, video.durationSeconds)

        val mediaGroup = quotedMessagePreviewModel(
            message(
                MessageType.MEDIA_GROUP,
                attachments = listOf(
                    MessageAttachment("image-url", AttachmentType.IMAGE),
                    MessageAttachment("first-video", AttachmentType.VIDEO, duration = 30),
                    MessageAttachment("second-video", AttachmentType.VIDEO, duration = 45)
                )
            )
        )
        assertEquals(QuotedMessageKind.MEDIA, mediaGroup.kind)
        assertEquals(75, mediaGroup.durationSeconds)
    }

    @Test
    fun voiceAndAudioQuotesWorkWithoutMediaUrlAndRetainDuration() {
        val voice = quotedMessagePreviewModel(
            message(MessageType.VOICE, attachments = listOf(MessageAttachment("", AttachmentType.AUDIO, duration = 12)))
        )
        assertEquals(QuotedMessageKind.VOICE, voice.kind)
        assertNull(voice.thumbnailUrl)
        assertEquals(12, voice.durationSeconds)
        assertEquals(QuotedMessageKind.AUDIO, quotedMessagePreviewModel(message(MessageType.AUDIO)).kind)
    }

    @Test
    fun pollUsesQuestionAndFileQuotesNeverRevealStoragePath() {
        val poll = quotedMessagePreviewModel(
            message(
                MessageType.POLL,
                metadata = MessageMetadataContainer(poll = PollMetadata("poll-id", "Which design?"))
            )
        )
        assertEquals("Which design?", poll.excerpt)

        val pathFile = quotedMessagePreviewModel(
            message(
                MessageType.FILE,
                content = "/private/cache/account-report.pdf",
                attachments = listOf(MessageAttachment("https://storage.example/opaque-id", AttachmentType.FILE, size = 2048))
            )
        )
        assertEquals(QuotedMessageKind.FILE, pathFile.kind)
        assertNull(pathFile.excerpt)
        assertEquals(2048L, pathFile.fileSizeBytes)
        assertEquals("account-report.pdf", quotedMessagePreviewModel(message(MessageType.FILE, content = "account-report.pdf")).excerpt)
        val hexFilename = "a".repeat(24) + ".txt"
        assertEquals(hexFilename, quotedMessagePreviewModel(message(MessageType.FILE, content = hexFilename)).excerpt)
        val uuidFilename = "550e8400-e29b-41d4-a716-446655440000.pdf"
        assertEquals(uuidFilename, quotedMessagePreviewModel(message(MessageType.FILE, content = uuidFilename)).excerpt)
        assertNull(quotedMessagePreviewModel(message(MessageType.FILE, content = "a".repeat(24))).excerpt)
    }

    @Test
    fun missingAndDeletedOriginalsReturnHonestPlaceholders() {
        assertEquals(QuotedMessageKind.UNAVAILABLE, quotedMessagePreviewModel(null).kind)
        val deleted = quotedMessagePreviewModel(message(MessageType.IMAGE, mediaUrl = "https://cdn.example/photo.jpg", deleted = true))
        assertEquals(QuotedMessageKind.DELETED, deleted.kind)
        assertNull(deleted.thumbnailUrl)
        assertNull(deleted.excerpt)
    }

    @Test
    fun longTextIsRetainedForUiEllipsis() {
        val text = "Important update ".repeat(80)
        assertEquals(text, quotedMessagePreviewModel(message(MessageType.TEXT, content = text)).excerpt)
    }

    @Test
    fun senderResolvesToYouForOwnMessagesAndNeverToTechnicalId() {
        val own = message(MessageType.TEXT).copy(senderId = "current-user")
        assertEquals("You", resolveQuotedSenderName(own, "current-user", "Stale label", "You", "Unknown sender", "Original message"))
        assertEquals("Alex Morgan", resolveQuotedSenderName(own.copy(senderId = "other"), "current-user", "Alex Morgan", "You", "Unknown sender", "Original message"))
        assertEquals("Unknown sender", resolveQuotedSenderName(own.copy(senderId = "other"), "current-user", null, "You", "Unknown sender", "Original message"))
        assertTrue(resolveQuotedSenderName(null, "current-user", null, "You", "Unknown sender", "Original message").isNotBlank())
    }

    @Test
    fun senderDisplayResolutionIsSharedByComposerAndMessageList() {
        val own = message(MessageType.TEXT).copy(senderId = "current-user")
        val other = own.copy(senderId = "member-1")
        assertEquals(
            "Alex",
            quotedSenderDisplayName(other, "current-user", false, emptyMap(), "Alex", null)
        )
        assertEquals(
            "Group member",
            quotedSenderDisplayName(other, "current-user", true, mapOf("member-1" to "Group member"), null, "Ignored group title")
        )
        assertNull(quotedSenderDisplayName(own, "current-user", false, emptyMap(), "Stale label", null))
    }

}
