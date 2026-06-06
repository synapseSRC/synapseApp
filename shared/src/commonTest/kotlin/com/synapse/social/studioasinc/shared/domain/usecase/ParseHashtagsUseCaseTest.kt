package com.synapse.social.studioasinc.shared.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseHashtagsUseCaseTest {
    private val useCase = ParseHashtagsUseCase()

    @Test
    fun shouldExtractSingleHashtag() {
        val text = "Hello #kotlin world"
        val result = useCase(text)
        assertEquals(listOf("kotlin"), result)
    }

    @Test
    fun shouldExtractMultipleHashtags() {
        val text = "Hello #kotlin and #android"
        val result = useCase(text)
        assertEquals(listOf("kotlin", "android"), result)
    }

    @Test
    fun shouldSkipPureNumberHashtags() {
        val text = "This is #123 and #kotlin"
        val result = useCase(text)
        assertEquals(listOf("kotlin"), result)
    }

    @Test
    fun shouldSupportUnderscores() {
        val text = "Love #jetpack_compose"
        val result = useCase(text)
        assertEquals(listOf("jetpack_compose"), result)
    }

    @Test
    fun shouldHandleDuplicates() {
        val text = "#kotlin is better than #kotlin"
        val result = useCase(text)
        assertEquals(listOf("kotlin"), result)
    }

    @Test
    fun shouldReturnEmptyForNoHashtags() {
        val text = "No hashtags here"
        val result = useCase(text)
        assertTrue(result.isEmpty())
    }

    @Test
    fun shouldHandleHashtagsWithNumbers() {
        val text = "Check out #swift5 and #android12"
        val result = useCase(text)
        assertEquals(listOf("swift5", "android12"), result)
    }
}
