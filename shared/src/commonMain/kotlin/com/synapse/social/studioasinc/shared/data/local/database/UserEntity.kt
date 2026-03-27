package com.synapse.social.studioasinc.shared.data.local.database

data class UserEntity(
    val uid: String,
    val username: String?,
    val email: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val website: String?,
    val location: String?,
    val isVerified: Boolean,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int
)
