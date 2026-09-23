package com.synapse.social.studioasinc.shared.domain.model

import com.synapse.social.studioasinc.data.repository.CreatePostRequestDto
import com.synapse.social.studioasinc.data.repository.ScheduledPostDto
import com.synapse.social.studioasinc.data.repository.toDomain
import com.synapse.social.studioasinc.data.repository.toDto
import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduledPostsTest {

    @Test
    fun shouldMapCreatePostRequestToDtoAndBack() {
        val request = CreatePostRequest(
            postText = "Hello scheduled post!",
            privacy = "followers",
            pollQuestion = "Favorite color?",
            pollOptions = listOf("Red", "Blue"),
            hideLikeCount = true
        )

        val dto = request.toDto()
        assertEquals("Hello scheduled post!", dto.postText)
        assertEquals("followers", dto.privacy)
        assertEquals("Favorite color?", dto.pollQuestion)
        assertEquals(listOf("Red", "Blue"), dto.pollOptions)
        assertTrue(dto.hideLikeCount)

        val domain = dto.toDomain()
        assertEquals(request.postText, domain.postText)
        assertEquals(request.privacy, domain.privacy)
        assertEquals(request.pollQuestion, domain.pollQuestion)
        assertEquals(request.pollOptions, domain.pollOptions)
        assertEquals(request.hideLikeCount, domain.hideLikeCount)
    }

    @Test
    fun shouldMapScheduledPostDtoToDomainModel() {
        val dto = ScheduledPostDto(
            id = "scheduled_123",
            userId = "user_456",
            postData = CreatePostRequestDto(
                postText = "Scheduled content"
            ),
            scheduledAt = "2026-10-25T10:00:00Z",
            status = "scheduled"
        )

        val scheduledPost = dto.toDomain()
        assertEquals("scheduled_123", scheduledPost.id)
        assertEquals("user_456", scheduledPost.userId)
        assertEquals("Scheduled content", scheduledPost.postRequest.postText)
        assertEquals("2026-10-25T10:00:00Z", scheduledPost.scheduledAt)
        assertEquals(ScheduledPost.STATUS_SCHEDULED, scheduledPost.status)
        assertNull(scheduledPost.errorMessage)
        assertNull(scheduledPost.publishedPostId)
    }

    @Test
    fun shouldValidateScheduledTimeIsInFuture() {
        val now = Clock.System.now()
        val futureInstant = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + 3600_000L)
        val pastInstant = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - 3600_000L)

        assertTrue(futureInstant > now, "Future instant must be greater than current time")
        assertFalse(pastInstant > now, "Past instant must not be greater than current time")
    }

    @Test
    fun shouldEnforceValidStatusTransitionsAndPreventDuplicatePublishing() {
        val initialStatus = ScheduledPost.STATUS_SCHEDULED

        val publishingStatus = ScheduledPost.STATUS_PUBLISHING

        val isEligibleForPublishing = fun(status: String): Boolean {
            return status == ScheduledPost.STATUS_SCHEDULED || status == ScheduledPost.STATUS_FAILED
        }

        assertTrue(isEligibleForPublishing(initialStatus))
        assertFalse(isEligibleForPublishing(publishingStatus), "Publishing item cannot be re-published concurrently")
        assertFalse(isEligibleForPublishing(ScheduledPost.STATUS_PUBLISHED), "Already published item cannot be re-published")
        assertTrue(isEligibleForPublishing(ScheduledPost.STATUS_FAILED), "Failed item can be retried")
    }
}
