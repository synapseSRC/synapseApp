package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.model.NotificationActorDto
import com.synapse.social.studioasinc.shared.data.model.NotificationDto
import com.synapse.social.studioasinc.shared.domain.model.NotificationTarget
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationMapperTest {

    @Test
    fun `actorName falls back from display_name to username then null`() {
        val dtoWithDisplayName = createNotificationDto(
            type = "NEW_POST",
            actor = NotificationActorDto(displayName = "Jane Doe", username = "janedoe")
        )
        assertEquals("Jane Doe", dtoWithDisplayName.toDomain().actorName)

        val dtoWithUsernameOnly = createNotificationDto(
            type = "NEW_POST",
            actor = NotificationActorDto(displayName = "", username = "janedoe")
        )
        assertEquals("janedoe", dtoWithUsernameOnly.toDomain().actorName)

        val dtoWithNoActorName = createNotificationDto(
            type = "NEW_POST",
            actor = NotificationActorDto(displayName = null, username = null)
        )
        assertNull(dtoWithNoActorName.toDomain().actorName)
    }

    @Test
    fun `maps NEW_POST to Post target`() {
        val dto = createNotificationDto(
            type = "NEW_POST",
            data = buildJsonObject {
                put("postId", "post-123")
                put("targetType", "POST")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Post("post-123"), domain.target)
    }

    @Test
    fun `maps NEW_COMMENT to Comment target`() {
        val dto = createNotificationDto(
            type = "NEW_COMMENT",
            data = buildJsonObject {
                put("postId", "post-123")
                put("commentId", "comment-456")
                put("targetType", "POST_COMMENT")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Comment("post-123", "comment-456"), domain.target)
    }

    @Test
    fun `maps NEW_REPLY to Comment target`() {
        val dto = createNotificationDto(
            type = "NEW_REPLY",
            data = buildJsonObject {
                put("postId", "post-123")
                put("commentId", "reply-789")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Comment("post-123", "reply-789"), domain.target)
    }

    @Test
    fun `maps NEW_LIKE_POST to Post target`() {
        val dto = createNotificationDto(
            type = "NEW_LIKE_POST",
            data = buildJsonObject {
                put("postId", "post-123")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Post("post-123"), domain.target)
    }

    @Test
    fun `maps NEW_LIKE_COMMENT to Comment target`() {
        val dto = createNotificationDto(
            type = "NEW_LIKE_COMMENT",
            data = buildJsonObject {
                put("postId", "post-123")
                put("commentId", "comment-456")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Comment("post-123", "comment-456"), domain.target)
    }

    @Test
    fun `maps MENTION to Post target`() {
        val dto = createNotificationDto(
            type = "MENTION",
            data = buildJsonObject {
                put("postId", "post-123")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Post("post-123"), domain.target)
    }

    @Test
    fun `maps NEW_FOLLOWER to Profile target`() {
        val dto = createNotificationDto(
            type = "NEW_FOLLOWER",
            senderId = "user-789",
            data = buildJsonObject {
                put("followerId", "user-789")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Profile("user-789"), domain.target)
    }

    @Test
    fun `maps legacy target_id fallback`() {
        val dto = createNotificationDto(
            type = "NEW_POST",
            data = buildJsonObject {
                put("target_id", "legacy-post-id")
            }
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Post("legacy-post-id"), domain.target)
    }

    @Test
    fun `handles malformed missing target gracefully without crashing`() {
        val dto = createNotificationDto(
            type = "UNKNOWN_TYPE",
            data = null
        )
        val domain = dto.toDomain()
        assertEquals(NotificationTarget.Unknown, domain.target)
    }

    private fun createNotificationDto(
        type: String,
        senderId: String? = "sender-1",
        actor: NotificationActorDto? = NotificationActorDto(displayName = "Test User"),
        data: kotlinx.serialization.json.JsonObject? = null
    ) = NotificationDto(
        id = "notif-1",
        recipientId = "recip-1",
        senderId = senderId,
        type = type,
        createdAt = "2026-10-05T22:00:00Z",
        actor = actor,
        data = data
    )
}
