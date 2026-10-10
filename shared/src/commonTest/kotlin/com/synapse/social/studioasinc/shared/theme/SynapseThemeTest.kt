package com.synapse.social.studioasinc.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SynapseThemeTest {

    @Test
    fun testBrandPrimaryIsBlueBased() {
        assertEquals(0xFF0061A4, SynapseColors.Light.primary)
        assertEquals(0xFF9ECAFF, SynapseColors.Dark.primary)
        assertNotEquals(0xFF6750A4, SynapseColors.Light.primary)
        assertNotEquals(0xFFD0BCFF, SynapseColors.Dark.primary)
    }

    @Test
    fun testSurfaceContainersArePopulated() {
        assertNotEquals(0L, SynapseColors.Light.surfaceContainerLowest)
        assertNotEquals(0L, SynapseColors.Light.surfaceContainerLow)
        assertNotEquals(0L, SynapseColors.Light.surfaceContainer)
        assertNotEquals(0L, SynapseColors.Light.surfaceContainerHigh)
        assertNotEquals(0L, SynapseColors.Light.surfaceContainerHighest)

        assertNotEquals(0L, SynapseColors.Dark.surfaceContainerLowest)
        assertNotEquals(0L, SynapseColors.Dark.surfaceContainerLow)
        assertNotEquals(0L, SynapseColors.Dark.surfaceContainer)
        assertNotEquals(0L, SynapseColors.Dark.surfaceContainerHigh)
        assertNotEquals(0L, SynapseColors.Dark.surfaceContainerHighest)
    }

    @Test
    fun testSemanticStatusColorsAreDefined() {
        assertEquals(0xFF2E7D32, SynapseColors.Light.statusOnline)
        assertEquals(0xFF81C784, SynapseColors.Dark.statusOnline)
        assertEquals(0xFF757575, SynapseColors.Light.statusOffline)
        assertEquals(0xFFBDBDBD, SynapseColors.Dark.statusOffline)
    }
}
