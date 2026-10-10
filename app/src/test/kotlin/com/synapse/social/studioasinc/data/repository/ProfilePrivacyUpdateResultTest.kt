package com.synapse.social.studioasinc.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePrivacyUpdateResultTest {
    @Test
    fun zeroRowUpdateIsFailureAndNeverSuccess() {
        val result = privacyUpdateResult(updated = false)
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun verifiedUpdateIsSuccess() {
        val result = privacyUpdateResult(updated = true)
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }
}
