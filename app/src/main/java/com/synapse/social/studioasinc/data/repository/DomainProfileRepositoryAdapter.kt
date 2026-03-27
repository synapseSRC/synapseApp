package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.UserProfile as DomainUserProfile
import com.synapse.social.studioasinc.domain.repository.ProfileRepository as DomainProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Adapts the data-layer ProfileRepository to the domain ProfileRepository interface,
 * mapping data.model.UserProfile ↔ domain.model.UserProfile.
 */
class DomainProfileRepositoryAdapter @Inject constructor(
    private val delegate: ProfileRepository
) : DomainProfileRepository {

    override fun getProfile(userId: String, refresh: Boolean): Flow<Result<DomainUserProfile>> =
        delegate.getProfile(userId, refresh).map { result -> result.map { it.toDomain() } }

    override suspend fun updateProfile(userId: String, profile: DomainUserProfile): Result<DomainUserProfile> =
        delegate.updateProfile(userId, profile.toData()).map { it.toDomain() }

    override suspend fun followUser(userId: String, targetUserId: String): Result<Unit> =
        delegate.followUser(userId, targetUserId)

    override suspend fun unfollowUser(userId: String, targetUserId: String): Result<Unit> =
        delegate.unfollowUser(userId, targetUserId)

    override suspend fun getFollowers(userId: String, limit: Int, offset: Int): Result<List<DomainUserProfile>> =
        delegate.getFollowers(userId, limit, offset).map { list -> list.map { it.toDomain() } }

    override suspend fun getFollowing(userId: String, limit: Int, offset: Int): Result<List<DomainUserProfile>> =
        delegate.getFollowing(userId, limit, offset).map { list -> list.map { it.toDomain() } }

    override suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<Post>> =
        delegate.getProfilePosts(userId, limit, offset)

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<MediaItem>> =
        delegate.getProfilePhotos(userId, limit, offset).map { list ->
            list.map { com.synapse.social.studioasinc.domain.model.MediaItem(
                id = it.id,
                url = it.url,
                type = com.synapse.social.studioasinc.domain.model.MediaType.IMAGE,
                thumbnailUrl = it.thumbnailUrl
            ) }
        }

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<MediaItem>> =
        delegate.getProfileReels(userId, limit, offset).map { list ->
            list.map { com.synapse.social.studioasinc.domain.model.MediaItem(
                id = it.id,
                url = it.url,
                type = com.synapse.social.studioasinc.domain.model.MediaType.VIDEO,
                thumbnailUrl = it.thumbnailUrl
            ) }
        }

    override suspend fun getProfileReplies(userId: String, limit: Int, offset: Int): Result<List<CommentWithUser>> =
        delegate.getProfileReplies(userId, limit, offset)

    override suspend fun isFollowing(userId: String, targetUserId: String): Result<Boolean> =
        delegate.isFollowing(userId, targetUserId)
}

private fun com.synapse.social.studioasinc.data.model.UserProfile.toDomain(): DomainUserProfile =
    DomainUserProfile(
        uid = id,
        username = username,
        displayName = name,
        bio = bio,
        avatar = avatar,
        profileCoverImage = coverImageUrl,
        verify = isVerified,
        followersCount = followerCount,
        followingCount = followingCount,
        postsCount = postCount,
        currentCity = currentCity,
        hometown = hometown,
        occupation = occupation,
        workplace = workplace,
        education = education?.let { listOf(it) } ?: emptyList(),
        birthday = birthday,
        relationshipStatus = relationshipStatus,
        discordTag = discordTag,
        githubProfile = githubProfile,
        personalWebsite = personalWebsite,
        publicEmail = publicEmail,
        pronouns = pronouns,
        gender = gender?.let { runCatching { Gender.valueOf(it.uppercase()) }.getOrNull() }
    )

internal fun DomainUserProfile.toData(): com.synapse.social.studioasinc.data.model.UserProfile =
    com.synapse.social.studioasinc.data.model.UserProfile(
        id = uid,
        username = username,
        name = displayName,
        bio = bio,
        avatar = avatar,
        coverImageUrl = profileCoverImage,
        isVerified = verify,
        followerCount = followersCount,
        followingCount = followingCount,
        postCount = postsCount,
        currentCity = currentCity,
        hometown = hometown,
        occupation = occupation,
        workplace = workplace,
        education = education.firstOrNull(),
        birthday = birthday,
        relationshipStatus = relationshipStatus,
        discordTag = discordTag,
        githubProfile = githubProfile,
        personalWebsite = personalWebsite,
        publicEmail = publicEmail,
        pronouns = pronouns,
        gender = gender?.name?.lowercase()
    )
