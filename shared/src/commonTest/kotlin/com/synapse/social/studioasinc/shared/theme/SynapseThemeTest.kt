package com.synapse.social.studioasinc.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SynapseThemeTest {

    @Test
    fun testBrandPrimaryIsBlueBased() {
        assertEquals(0xFF0061A4, SynapseColors.Light.primary)
        assertEquals(0xFF9ECAFF, SynapseColors.Dark.primary)
        assertEquals(0xFF0061A4, SynapseColors.Blue)
        assertNotEquals(0xFF6750A4, SynapseColors.Light.primary)
        assertNotEquals(0xFFD0BCFF, SynapseColors.Dark.primary)
    }

    @Test
    fun testSurfaceContainersArePopulated() {
        assertEquals(0xFFFFFFFF, SynapseColors.Light.surfaceContainerLowest)
        assertEquals(0xFFF2F3FA, SynapseColors.Light.surfaceContainerLow)
        assertEquals(0xFFECEEF6, SynapseColors.Light.surfaceContainer)
        assertEquals(0xFFE6E8F0, SynapseColors.Light.surfaceContainerHigh)
        assertEquals(0xFFE0E2EC, SynapseColors.Light.surfaceContainerHighest)

        assertEquals(0xFF0C0E13, SynapseColors.Dark.surfaceContainerLowest)
        assertEquals(0xFF191C20, SynapseColors.Dark.surfaceContainerLow)
        assertEquals(0xFF1D2024, SynapseColors.Dark.surfaceContainer)
        assertEquals(0xFF282A2F, SynapseColors.Dark.surfaceContainerHigh)
        assertEquals(0xFF33353A, SynapseColors.Dark.surfaceContainerHighest)
    }

    @Test
    fun testSemanticStatusColorsAreDefined() {
        assertEquals(0xFF2E7D32, SynapseColors.Light.statusOnline)
        assertEquals(0xFF81C784, SynapseColors.Dark.statusOnline)
        assertEquals(0xFF757575, SynapseColors.Light.statusOffline)
        assertEquals(0xFFBDBDBD, SynapseColors.Dark.statusOffline)
        assertEquals(0xFF1B6D33, SynapseColors.Light.success)
        assertEquals(0xFF8CDB96, SynapseColors.Dark.success)
    }
}
