package com.synapse.social.studioasinc.feature.profile.profile

internal fun canViewProfileContent(
    isPrivate: Boolean,
    isOwnProfile: Boolean,
    isFollowing: Boolean
): Boolean = isOwnProfile || !isPrivate || isFollowing

internal fun isPrivateProfileLockedForViewer(
    isPrivate: Boolean,
    isOwnProfile: Boolean,
    isFollowing: Boolean
): Boolean = isPrivate && !isOwnProfile && !isFollowing
