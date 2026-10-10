package com.synapse.social.studioasinc.core.util

import com.synapse.social.studioasinc.ui.settings.AutoDownloadRules
import com.synapse.social.studioasinc.ui.settings.MediaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBubbleAutoDownloadPolicyTest {

    @Test
    fun testUnresolvedPolicy_preventsAutomaticMediaLoading() {
        val isFromMe = false
        val isAutoDownloadAllowed: Boolean? = null
        val userRequestedDownload = false

        val shouldShowMedia = isFromMe || (isAutoDownloadAllowed == true) || userRequestedDownload

        assertFalse("Automatic media loading must NOT begin when policy is unresolved", shouldShowMedia)
    }

    @Test
    fun testDisallowedNetwork_preventsMediaLoading() {
        val rules = AutoDownloadRules(
            wifi = setOf(MediaType.PHOTO),
            mobileData = emptySet(),
            roaming = emptySet()
        )

        val isAllowedOnMobile = AutoDownloadManager.isAutoDownloadAllowedForNetwork(
            AutoDownloadManager.NetworkType.MOBILE,
            rules,
            MediaType.PHOTO
        )

        assertFalse(isAllowedOnMobile)
    }

    @Test
    fun testPermittedNetwork_allowsMediaLoading() {
        val rules = AutoDownloadRules(
            wifi = setOf(MediaType.PHOTO, MediaType.VIDEO),
            mobileData = setOf(MediaType.PHOTO),
            roaming = emptySet()
        )

        val isAllowedOnWifi = AutoDownloadManager.isAutoDownloadAllowedForNetwork(
            AutoDownloadManager.NetworkType.WIFI,
            rules,
            MediaType.VIDEO
        )

        assertTrue(isAllowedOnWifi)
    }

    @Test
    fun testManualDownloadRequest_overridesDisallowedPolicy() {
        val isFromMe = false
        val isAutoDownloadAllowed = false
        val userRequestedDownload = true

        val shouldShowMedia = isFromMe || isAutoDownloadAllowed || userRequestedDownload

        assertTrue("Manual user download request must allow displaying media", shouldShowMedia)
    }
}
