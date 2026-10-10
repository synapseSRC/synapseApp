package com.synapse.social.studioasinc.feature.shared.components.post

import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaContentTest {

    @Test
    fun testLandscapeImageDisplayHeightNotCapped() {
        // Landscape image: 16:9 (1920x1080)
        val intrinsicWidth = 1920f
        val intrinsicHeight = 1080f
        val containerWidth = 360f
        val maxHeight = Sizes.HeightMediaSingle.value

        val displayHeight = calculateMediaDisplayHeight(
            intrinsicWidth = intrinsicWidth,
            intrinsicHeight = intrinsicHeight,
            containerWidth = containerWidth,
            maxHeight = maxHeight
        )

        val expectedHeight = 360f / (1920f / 1080f) // 202.5f
        assertEquals(expectedHeight, displayHeight, 0.01f)
        assertTrue(displayHeight < maxHeight)
    }

    @Test
    fun testSquareImageDisplayHeightNotCapped() {
        // Square image: 1:1 (1080x1080)
        val intrinsicWidth = 1080f
        val intrinsicHeight = 1080f
        val containerWidth = 360f
        val maxHeight = Sizes.HeightMediaSingle.value

        val displayHeight = calculateMediaDisplayHeight(
            intrinsicWidth = intrinsicWidth,
            intrinsicHeight = intrinsicHeight,
            containerWidth = containerWidth,
            maxHeight = maxHeight
        )

        assertEquals(360f, displayHeight, 0.01f)
        assertTrue(displayHeight <= maxHeight)
    }

    @Test
    fun testNormalPortraitImageDisplayHeightCappedAtMax() {
        // Moderate portrait image: 4:5 (1080x1350)
        val intrinsicWidth = 1080f
        val intrinsicHeight = 1350f
        val containerWidth = 360f
        val maxHeight = Sizes.HeightMediaSingle.value

        val displayHeight = calculateMediaDisplayHeight(
            intrinsicWidth = intrinsicWidth,
            intrinsicHeight = intrinsicHeight,
            containerWidth = containerWidth,
            maxHeight = maxHeight
        )

        // Ideal height = 360 / (1080/1350) = 450f, capped at maxHeight (400f)
        assertEquals(maxHeight, displayHeight, 0.01f)
    }

    @Test
    fun testExtremePortraitImageDisplayHeightCappedAtMax() {
        // Extreme portrait image: 1:3 (1000x3000) wallpaper
        val intrinsicWidth = 1000f
        val intrinsicHeight = 3000f
        val containerWidth = 360f
        val maxHeight = Sizes.HeightMediaSingle.value

        val displayHeight = calculateMediaDisplayHeight(
            intrinsicWidth = intrinsicWidth,
            intrinsicHeight = intrinsicHeight,
            containerWidth = containerWidth,
            maxHeight = maxHeight
        )

        // Ideal height = 360 / (1000/3000) = 1080f, capped at maxHeight (400f)
        assertEquals(maxHeight, displayHeight, 0.01f)
    }
}
