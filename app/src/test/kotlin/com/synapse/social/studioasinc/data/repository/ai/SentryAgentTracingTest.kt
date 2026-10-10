package com.synapse.social.studioasinc.data.repository.ai

import org.junit.Assert.*
import org.junit.Test

class SentryAgentTracingTest {
    @Test fun conversationIdsAreHashedAndStable() {
        val raw = "123e4567-e89b-12d3-a456-426614174000"
        val hashed = SentryAgentTracing.conversationIdForSentry(raw)
        assertNotNull(hashed)
        assertEquals(hashed, SentryAgentTracing.conversationIdForSentry(raw))
        assertNotEquals(raw, hashed)
        assertEquals(64, hashed!!.length)
    }
    @Test fun freeFormConversationIdentifiersAreOmitted() {
        assertNull(SentryAgentTracing.conversationIdForSentry("person@example.com"))
        assertNull(SentryAgentTracing.conversationIdForSentry("chat id with spaces"))
    }
    @Test fun sanitizedExceptionOmitsOriginalMessageAndKeepsStack() {
        val original = IllegalStateException("private prompt: person@example.com")
        val sanitized = SentryAgentTracing.sanitizedException(original)
        assertFalse(sanitized.message.orEmpty().contains("person@example.com"))
        assertEquals(original.stackTrace.toList(), sanitized.stackTrace.toList())
    }
}
