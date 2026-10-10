package com.synapse.social.studioasinc.core.util

import com.synapse.social.studioasinc.ui.settings.AutoDownloadRules
import com.synapse.social.studioasinc.ui.settings.MediaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoDownloadManagerTest {

    @Test
    fun testIsAutoDownloadAllowedForNetwork_wifiAllowed() {
        val rules = AutoDownloadRules(
            wifi = setOf(MediaType.PHOTO, MediaType.VIDEO),
            mobileData = setOf(MediaType.PHOTO),
            roaming = emptySet()
        )

        assertTrue(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.WIFI,
                rules,
                MediaType.PHOTO
            )
        )
        assertTrue(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.WIFI,
                rules,
                MediaType.VIDEO
            )
        )
        assertFalse(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.WIFI,
                rules,
                MediaType.AUDIO
            )
        )
    }

    @Test
    fun testIsAutoDownloadAllowedForNetwork_mobileDataRestrictions() {
        val rules = AutoDownloadRules(
            wifi = setOf(MediaType.PHOTO, MediaType.VIDEO),
            mobileData = setOf(MediaType.PHOTO),
            roaming = emptySet()
        )

        assertTrue(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.MOBILE,
                rules,
                MediaType.PHOTO
            )
        )
        assertFalse(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.MOBILE,
                rules,
                MediaType.VIDEO
            )
        )
    }

    @Test
    fun testIsAutoDownloadAllowedForNetwork_roamingDisallowed() {
        val rules = AutoDownloadRules(
            wifi = MediaType.values().toSet(),
            mobileData = setOf(MediaType.PHOTO),
            roaming = emptySet()
        )

        assertFalse(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.ROAMING,
                rules,
                MediaType.PHOTO
            )
        )
    }

    @Test
    fun testIsAutoDownloadAllowedForNetwork_noneNetworkDisallowed() {
        val rules = AutoDownloadRules(
            wifi = MediaType.values().toSet(),
            mobileData = MediaType.values().toSet(),
            roaming = MediaType.values().toSet()
        )

        assertFalse(
            AutoDownloadManager.isAutoDownloadAllowedForNetwork(
                AutoDownloadManager.NetworkType.NONE,
                rules,
                MediaType.PHOTO
            )
        )
    }
}
