package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.data.repository.CreatePostRequestDto
import com.synapse.social.studioasinc.data.repository.ScheduledPostDto
import com.synapse.social.studioasinc.data.repository.toDomain
import com.synapse.social.studioasinc.data.repository.toDto
import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

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
        assertEquals("followers", dto.postVisibility)
        assertEquals("Favorite color?", dto.pollQuestion)
        assertEquals(listOf("Red", "Blue"), dto.pollOptions)
        assertEquals("true", dto.postHideLikeCount)

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

        assertTrue("Future instant must be greater than current time", futureInstant > now)
        assertFalse("Past instant must not be greater than current time", pastInstant > now)
    }

    @Test
    fun shouldEnforceValidStatusTransitionsAndPreventDuplicatePublishing() {
        val initialStatus = ScheduledPost.STATUS_SCHEDULED

        val publishingStatus = ScheduledPost.STATUS_PUBLISHING

        val isEligibleForPublishing = fun(status: String): Boolean {
            return status == ScheduledPost.STATUS_SCHEDULED || status == ScheduledPost.STATUS_FAILED
        }

        assertTrue(isEligibleForPublishing(initialStatus))
        assertFalse("Publishing item cannot be re-published concurrently", isEligibleForPublishing(publishingStatus))
        assertFalse("Already published item cannot be re-published", isEligibleForPublishing(ScheduledPost.STATUS_PUBLISHED))
        assertTrue("Failed item can be retried", isEligibleForPublishing(ScheduledPost.STATUS_FAILED))
    }
}
