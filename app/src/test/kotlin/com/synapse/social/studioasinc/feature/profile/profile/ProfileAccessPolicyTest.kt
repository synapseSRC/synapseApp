package com.synapse.social.studioasinc.feature.profile.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAccessPolicyTest {
    @Test fun publicProfileIsVisibleWithoutFollow() {
        assertTrue(canViewProfileContent(isPrivate = false, isOwnProfile = false, isFollowing = false))
        assertFalse(isPrivateProfileLockedForViewer(isPrivate = false, isOwnProfile = false, isFollowing = false))
    }

    @Test fun privateProfileRequiresOwnerOrApprovedFollow() {
        assertFalse(canViewProfileContent(isPrivate = true, isOwnProfile = false, isFollowing = false))
        assertTrue(isPrivateProfileLockedForViewer(isPrivate = true, isOwnProfile = false, isFollowing = false))
        assertTrue(canViewProfileContent(isPrivate = true, isOwnProfile = true, isFollowing = false))
        assertTrue(canViewProfileContent(isPrivate = true, isOwnProfile = false, isFollowing = true))
        assertFalse(isPrivateProfileLockedForViewer(isPrivate = true, isOwnProfile = false, isFollowing = true))
    }
}
