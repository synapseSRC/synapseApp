package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SynapseThemeTest {

    @Test
    fun testBrandPrimaryIsBlueBased() {
        assertEquals(Color(0xFF0061A4), LightPrimary)
        assertEquals(Color(0xFF9ECAFF), DarkPrimary)

        assertNotEquals(Color(0xFF6750A4), LightPrimary)
        assertNotEquals(Color(0xFFD0BCFF), DarkPrimary)
    }

    @Test
    fun testSurfaceContainerHierarchyIsDefined() {
        assertEquals(Color(0xFFFFFFFF), LightSurfaceContainerLowest)
        assertEquals(Color(0xFFF2F3FA), LightSurfaceContainerLow)
        assertEquals(Color(0xFFECEEF6), LightSurfaceContainer)
        assertEquals(Color(0xFFE6E8F0), LightSurfaceContainerHigh)
        assertEquals(Color(0xFFE0E2EC), LightSurfaceContainerHighest)

        assertEquals(Color(0xFF0C0E13), DarkSurfaceContainerLowest)
        assertEquals(Color(0xFF191C20), DarkSurfaceContainerLow)
        assertEquals(Color(0xFF1D2024), DarkSurfaceContainer)
        assertEquals(Color(0xFF282A2F), DarkSurfaceContainerHigh)
        assertEquals(Color(0xFF33353A), DarkSurfaceContainerHighest)
    }

    @Test
    fun testSafeParseColorHexFallbackOnNullOrBlank() {
        val fallback = Color.Red
        assertEquals(fallback, safeParseColorHex(null, fallback))
        assertEquals(fallback, safeParseColorHex("", fallback))
        assertEquals(fallback, safeParseColorHex("   ", fallback))
        assertEquals(fallback, safeParseColorHex("invalid_color", fallback))
    }
}
