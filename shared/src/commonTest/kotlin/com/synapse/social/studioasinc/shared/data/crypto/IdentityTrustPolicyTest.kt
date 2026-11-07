package com.synapse.social.studioasinc.shared.data.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentityTrustPolicyTest {

    @Test
    fun firstKeySeenIsAcceptedOnce() {
        assertEquals(IdentityTrustOutcome.FIRST_USE, IdentityTrustPolicy.decide(null, "key-a"))
        assertTrue(IdentityTrustPolicy.isTrusted(null, "key-a"))
    }

    @Test
    fun sameKeyStaysTrusted() {
        assertEquals(IdentityTrustOutcome.UNCHANGED, IdentityTrustPolicy.decide("key-a", "key-a"))
        assertTrue(IdentityTrustPolicy.isTrusted("key-a", "key-a"))
    }

    @Test
    fun changedKeyIsRefused() {
        assertEquals(IdentityTrustOutcome.CHANGED, IdentityTrustPolicy.decide("key-a", "key-b"))
        assertFalse(IdentityTrustPolicy.isTrusted("key-a", "key-b"))
        assertFalse(IdentityTrustOutcome.CHANGED.isUsable)
    }

    @Test
    fun blankStoredKeyIsTreatedAsFirstUse() {
        assertEquals(IdentityTrustOutcome.FIRST_USE, IdentityTrustPolicy.decide("", "key-a"))
        assertEquals(IdentityTrustOutcome.FIRST_USE, IdentityTrustPolicy.decide("   ", "key-a"))
    }

    @Test
    fun blankPresentedKeyIsNeverTrusted() {
        assertEquals(IdentityTrustOutcome.CHANGED, IdentityTrustPolicy.decide("key-a", ""))
        assertFalse(IdentityTrustPolicy.isTrusted(null, "  "))
    }
}
