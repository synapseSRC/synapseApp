package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StackedMediaCardLayoutTest {

    @Test
    fun cardUsesCompactLandscapeBoundsWithinStandardChatBubble() {
        val dimensions = stackedMediaCardDimensions(280.dp)

        assertEquals(280f, dimensions.stackWidth.value, 0.001f)
        assertEquals(232f, dimensions.cardWidth.value, 0.001f)
        assertEquals(174f, dimensions.cardHeight.value, 0.001f)
        assertEquals(202f, dimensions.stackHeight.value, 0.001f)
        assertTrue(dimensions.stackWidth <= 280.dp)
    }

    @Test
    fun cardShrinksWithNarrowBubbleWidth() {
        val dimensions = stackedMediaCardDimensions(240.dp)

        assertEquals(240f, dimensions.stackWidth.value, 0.001f)
        assertEquals(192f, dimensions.cardWidth.value, 0.001f)
        assertEquals(144f, dimensions.cardHeight.value, 0.001f)
        assertEquals(172f, dimensions.stackHeight.value, 0.001f)
        assertTrue(dimensions.stackWidth <= 240.dp)
    }

    @Test
    fun cardStopsGrowingAndKeepsRotationReserveOnWideScreens() {
        val dimensions = stackedMediaCardDimensions(360.dp)

        assertEquals(320f, dimensions.stackWidth.value, 0.001f)
        assertEquals(272f, dimensions.cardWidth.value, 0.001f)
        assertEquals(204f, dimensions.cardHeight.value, 0.001f)
        assertEquals(232f, dimensions.stackHeight.value, 0.001f)
    }

    @Test
    fun unboundedWidthUsesConfiguredMaximum() {
        val dimensions = stackedMediaCardDimensions(Dp.Infinity)

        assertEquals(320f, dimensions.stackWidth.value, 0.001f)
        assertEquals(272f, dimensions.cardWidth.value, 0.001f)
        assertEquals(204f, dimensions.cardHeight.value, 0.001f)
        assertEquals(232f, dimensions.stackHeight.value, 0.001f)
    }

    @Test
    fun cardDoesNotCreateNegativeSizeWhenParentIsNarrowerThanStackReserve() {
        val dimensions = stackedMediaCardDimensions(40.dp)

        assertEquals(40f, dimensions.stackWidth.value, 0.001f)
        assertEquals(0f, dimensions.cardWidth.value, 0.001f)
        assertEquals(0f, dimensions.cardHeight.value, 0.001f)
        assertEquals(0f, dimensions.stackHeight.value, 0.001f)
    }

    @Test
    fun visibleBackgroundCardCountMatchesAttachmentCount() {
        assertEquals(0, stackedBackgroundCardCount(1))
        assertEquals(1, stackedBackgroundCardCount(2))
        assertEquals(2, stackedBackgroundCardCount(3))
        assertEquals(2, stackedBackgroundCardCount(12))
    }

    @Test
    fun videoDurationUsesCompactClockFormatIncludingHours() {
        assertEquals("0:24", formatVideoDuration(24))
        assertEquals("1:05", formatVideoDuration(65))
        assertEquals("1:00:01", formatVideoDuration(3_601))
        assertEquals("0:00", formatVideoDuration(-5))
    }
}
