package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.domain.model.UserStatus
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.feature.profile.profile.utils.NetworkOptimizer
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import kotlinx.serialization.json.*

@Serializable
data class FollowInsert(
    val follower_id: String,
    val following_id: String
)

@Serializable
private data class FollowingIdResponse(
    val following_id: String
)


class ProfileRepositoryImpl(
    private val client: SupabaseClientType,
    private val commentRepository: CommentRepository
) : ProfileRepository {

    private val postsRepository = ProfilePostsRepository(
        client = client,
        commentRepository = commentRepository,
        constructMediaUrl = ::constructMediaUrl,
        constructAvatarUrl = ::constructAvatarUrl,
        resolveUserId = ::resolveUserId
    )

    companion object {

        internal const val KEY_UID = "uid"
        internal const val KEY_USERNAME = "username"
        internal const val KEY_DISPLAY_NAME = "display_name"
        internal const val KEY_BIO = "bio"
        internal const val KEY_AVATAR = "avatar"
        internal const val KEY_COVER_IMAGE = "profile_cover_image"
        internal const val KEY_VERIFY = "verify"
        internal const val KEY_STATUS = "status"
        internal const val KEY_IS_PRIVATE = "is_private"
        internal const val KEY_POSTS_COUNT = "posts_count"
        internal const val KEY_FOLLOWERS_COUNT = "followers_count"
        internal const val KEY_FOLLOWING_COUNT = "following_count"
        internal const val KEY_LOCATION = "location"
        internal const val KEY_WEBSITE = "website"
        internal const val KEY_GENDER = "gender"
        internal const val KEY_PRONOUNS = "pronouns"
        internal const val KEY_JOIN_DATE = "join_date"
        internal const val KEY_CURRENT_CITY = "current_city"
        internal const val KEY_HOMETOWN = "hometown"
        internal const val KEY_OCCUPATION = "occupation"
        internal const val KEY_WORKPLACE = "workplace"
        internal const val KEY_EDUCATION = "education"
        internal const val KEY_BIRTHDAY = "birthday"
        internal const val KEY_RELATIONSHIP_STATUS = "relationship_status"
        internal const val KEY_DISCORD_TAG = "discord_tag"
        internal const val KEY_GITHUB_PROFILE = "github_profile"
        internal const val KEY_PERSONAL_WEBSITE = "personal_website"
        internal const val KEY_PUBLIC_EMAIL = "public_email"
    }



    internal fun resolveUserId(userId: String): String? {
        return if (userId == "me") {
            client.auth.currentUserOrNull()?.id
        } else {
            userId
        }
    }

    internal fun constructMediaUrl(storagePath: String): String = SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, storagePath)

    internal fun constructAvatarUrl(storagePath: String): String = SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, storagePath)

    private fun parseUserProfile(data: JsonObject, postCount: Int = 0): UserProfile {
        return UserProfile(
            id = data.getString(KEY_UID, ""),
            username = data.getString(KEY_USERNAME),
            name = data.getNullableString(KEY_DISPLAY_NAME),
            bio = data.getNullableString(KEY_BIO),
            avatar = data.getNullableString(KEY_AVATAR)?.let { constructAvatarUrl(it) },
            coverImageUrl = data.getNullableString(KEY_COVER_IMAGE)?.let { constructMediaUrl(it) },
            isVerified = data.getBoolean(KEY_VERIFY),
            status = UserStatus.fromString(data.getNullableString(KEY_STATUS)),
            isPrivate = data.getBoolean(KEY_IS_PRIVATE),
            postCount = postCount,
            followerCount = data.getInt(KEY_FOLLOWERS_COUNT),
            followingCount = data.getInt(KEY_FOLLOWING_COUNT),
            location = data.getNullableString(KEY_LOCATION),
            website = data.getNullableString(KEY_WEBSITE),
            gender = data.getNullableString(KEY_GENDER),
            pronouns = data.getNullableString(KEY_PRONOUNS),
            joinedDate = parseDateToLong(data.getNullableString(KEY_JOIN_DATE)),
            currentCity = data.getNullableString(KEY_CURRENT_CITY),
            hometown = data.getNullableString(KEY_HOMETOWN),
            occupation = data.getNullableString(KEY_OCCUPATION),
            workplace = data.getNullableString(KEY_WORKPLACE),
            education = data.getNullableString(KEY_EDUCATION),
            birthday = data.getNullableString(KEY_BIRTHDAY),
            relationshipStatus = data.getNullableString(KEY_RELATIONSHIP_STATUS),
            discordTag = data.getNullableString(KEY_DISCORD_TAG),
            githubProfile = data.getNullableString(KEY_GITHUB_PROFILE),
            personalWebsite = data.getNullableString(KEY_PERSONAL_WEBSITE),
            publicEmail = data.getNullableString(KEY_PUBLIC_EMAIL),
            linkedAccounts = emptyList(),
            privacySettings = emptyMap()
        )
    }

    override fun getProfile(userId: String, refresh: Boolean): Flow<Result<UserProfile>> = flow {
        val actualUserId = resolveUserId(userId) ?: run {
            emit(Result.failure(Exception("User not authenticated")))
            return@flow
        }

        val cacheKey = "profile_$actualUserId"
        if (!refresh) {
            NetworkOptimizer.getCached<UserProfile>(cacheKey)?.let {
                emit(Result.success(it))
                return@flow
            }
        }

        try {
            android.util.Log.d("ProfileRepository", "Loading profile for userId: $actualUserId")
            val response = NetworkOptimizer.withRetry {
                client.from("users").select() {
                    filter { eq(KEY_UID, actualUserId) }
                }.decodeSingleOrNull<JsonObject>()
            }

            android.util.Log.d("ProfileRepository", "Profile query response: $response")

            if (response == null) {
                android.util.Log.e("ProfileRepository", "Profile not found for userId: $actualUserId")


                try {
                    val currentUser = client.auth.currentUserOrNull()
                    if (currentUser != null && currentUser.id == actualUserId) {
                        android.util.Log.d("ProfileRepository", "Attempting to create missing profile for current user")

                        val userMap = mapOf(
                            "uid" to actualUserId,
                            "username" to (currentUser.email?.substringBefore("@") ?: "user"),
                            "email" to (currentUser.email ?: ""),
                            "created_at" to java.time.Instant.now().toString(),
                            "join_date" to java.time.Instant.now().toString(),
                            "followers_count" to 0,
                            "following_count" to 0,
                            "posts_count" to 0,
                            "user_level_xp" to 0
                        )

                        client.from("users").insert(userMap)
                        android.util.Log.d("ProfileRepository", "Created missing profile, retrying query")


                        val retryResponse = client.from("users").select() {
                            filter { eq(KEY_UID, actualUserId) }
                        }.decodeSingleOrNull<JsonObject>()

                        if (retryResponse != null) {

                            val postCount = try {
                                client.from("posts").select(columns = Columns.raw("count")) {
                                    filter { eq(ProfilePostsRepository.KEY_AUTHOR_UID, actualUserId) }
                                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                                }.countOrNull() ?: 0
                            } catch (e: Exception) {
                                0
                            }

                            val profile = parseUserProfile(retryResponse, postCount.toInt())
                            NetworkOptimizer.cache(cacheKey, profile)
                            android.util.Log.d("ProfileRepository", "Profile created and loaded successfully")
                            emit(Result.success(profile))
                            return@flow
                        }
                    }
                } catch (createError: Exception) {
                    android.util.Log.e("ProfileRepository", "Failed to create missing profile", createError)
                }

                emit(Result.failure(Exception("Profile not found for user: $actualUserId")))
                return@flow
            }


            val postCount = try {
                client.from("posts").select(columns = Columns.raw("count")) {
                    filter { eq(ProfilePostsRepository.KEY_AUTHOR_UID, actualUserId) }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }.countOrNull() ?: 0
            } catch (e: Exception) {
                0
            }

            val profile = parseUserProfile(response, postCount.toInt())
            NetworkOptimizer.cache(cacheKey, profile)
            android.util.Log.d("ProfileRepository", "Profile loaded successfully for userId: $actualUserId")
            emit(Result.success(profile))
        } catch (e: Exception) {
            android.util.Log.e("ProfileRepository", "Failed to load profile for userId: $actualUserId", e)
            emit(Result.failure(Exception("Failed to load profile: ${e.message}", e)))
        }
    }

    override suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile> = try {
        val updated = client.from("users").update(profile) { filter { eq("uid", userId) } }.decodeSingle<UserProfile>()
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun followUser(userId: String, targetUserId: String): Result<Unit> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val actualTargetUserId = resolveUserId(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        client.from("follows").upsert(
            FollowInsert(follower_id = actualUserId, following_id = actualTargetUserId)
        ) {
            onConflict = "follower_id, following_id"
            ignoreDuplicates = true
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unfollowUser(userId: String, targetUserId: String): Result<Unit> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val actualTargetUserId = resolveUserId(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        client.from("follows").delete { filter { eq("follower_id", actualUserId); eq("following_id", actualTargetUserId) } }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowers(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val followers = client.from("follows").select() {
            filter { eq("follower_id", actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<Map<String, UserProfile>>().mapNotNull { it["following_id"] }
        Result.success(followers)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowing(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))


        val followingIds = client.from("follows").select(columns = Columns.raw("following_id")) {
            filter { eq("follower_id", actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<FollowingIdResponse>().map {
            it.following_id
        }

        if (followingIds.isEmpty()) {
            Result.success(emptyList())
        } else {

            val usersResponse = client.from("users").select {
                filter { isIn(KEY_UID, followingIds) }
            }.decodeList<JsonObject>()

            val profiles = usersResponse.map { parseUserProfile(it) }
            Result.success(profiles)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.domain.model.Post>> =
        postsRepository.getProfilePosts(userId, limit, offset)

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> =
        postsRepository.getProfilePhotos(userId, limit, offset)

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> =
        postsRepository.getProfileReels(userId, limit, offset)

    override suspend fun getProfileReplies(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.domain.model.CommentWithUser>> =
        postsRepository.getProfileReplies(userId, limit, offset)

    override suspend fun isFollowing(userId: String, targetUserId: String): Result<Boolean> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val actualTargetUserId = resolveUserId(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        val result = client.from("follows").select() {
            filter { eq("follower_id", actualUserId); eq("following_id", actualTargetUserId) }
        }.decodeList<JsonObject>()
        Result.success(result.isNotEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
