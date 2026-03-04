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


class ProfileRepositoryImpl(private val client: SupabaseClientType) : ProfileRepository {

    private val reactionRepository = ReactionRepository()
    private val pollRepository = PollRepository()

    private companion object {

        const val KEY_UID = "uid"
        const val KEY_USERNAME = "username"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_BIO = "bio"
        const val KEY_AVATAR = "avatar"
        const val KEY_COVER_IMAGE = "profile_cover_image"
        const val KEY_VERIFY = "verify"
        const val KEY_STATUS = "status"
        const val KEY_IS_PRIVATE = "is_private"
        const val KEY_POSTS_COUNT = "posts_count"
        const val KEY_FOLLOWERS_COUNT = "followers_count"
        const val KEY_FOLLOWING_COUNT = "following_count"
        const val KEY_LOCATION = "location"
        const val KEY_WEBSITE = "website"
        const val KEY_GENDER = "gender"
        const val KEY_PRONOUNS = "pronouns"
        const val KEY_JOIN_DATE = "join_date"
        const val KEY_CURRENT_CITY = "current_city"
        const val KEY_HOMETOWN = "hometown"
        const val KEY_OCCUPATION = "occupation"
        const val KEY_WORKPLACE = "workplace"
        const val KEY_EDUCATION = "education"
        const val KEY_BIRTHDAY = "birthday"
        const val KEY_RELATIONSHIP_STATUS = "relationship_status"
        const val KEY_DISCORD_TAG = "discord_tag"
        const val KEY_GITHUB_PROFILE = "github_profile"
        const val KEY_PERSONAL_WEBSITE = "personal_website"
        const val KEY_PUBLIC_EMAIL = "public_email"

        const val KEY_ID = "id"
        const val KEY_AUTHOR_UID = "author_uid"
        const val KEY_POST_TEXT = "post_text"
        const val KEY_POST_IMAGE = "post_image"
        const val KEY_POST_TYPE = "post_type"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_LIKES_COUNT = "likes_count"
        const val KEY_COMMENTS_COUNT = "comments_count"
        const val KEY_VIEWS_COUNT = "views_count"
        const val KEY_HAS_POLL = "has_poll"
        const val KEY_POLL_QUESTION = "poll_question"
        const val KEY_POLL_OPTIONS = "poll_options"
        const val KEY_MEDIA_ITEMS = "media_items"
        const val KEY_USERS = "users"
        const val KEY_URL = "url"
        const val KEY_TYPE = "type"
        const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        const val KEY_TEXT = "text"
        const val KEY_VOTES = "votes"


        const val MEDIA_TYPE_VIDEO = "VIDEO"
        const val MEDIA_TYPE_IMAGE = "IMAGE"
    }



    private fun resolveUserId(userId: String): String? {
        return if (userId == "me") {
            client.auth.currentUserOrNull()?.id
        } else {
            userId
        }
    }

    private fun constructMediaUrl(storagePath: String): String = SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, storagePath)

    private fun constructAvatarUrl(storagePath: String): String = SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, storagePath)

    private fun JsonObject.getString(key: String, default: String = ""): String =
        this[key]?.jsonPrimitive?.contentOrNull ?: default

    private fun JsonObject.getNullableString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.getBoolean(key: String, default: Boolean = false): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: default

    private fun JsonObject.getInt(key: String, default: Int = 0): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.getLong(key: String, default: Long = 0L): Long =
        this[key]?.jsonPrimitive?.longOrNull ?: default

    private fun parseDateToLong(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            java.time.Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

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
            publicEmail = data.getNullableString(KEY_PUBLIC_EMAIL)
        )
    }

    override fun getProfile(userId: String): Flow<Result<UserProfile>> = flow {

        val actualUserId = resolveUserId(userId) ?: run {
            emit(Result.failure(Exception("User not authenticated")))
            return@flow
        }

        val cacheKey = "profile_$actualUserId"
        NetworkOptimizer.getCached<UserProfile>(cacheKey)?.let {
            emit(Result.success(it))
            return@flow
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
                                    filter { eq(KEY_AUTHOR_UID, actualUserId) }
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
                    filter { eq(KEY_AUTHOR_UID, actualUserId) }
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

    override suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.domain.model.Post>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val response = client.from("posts").select(
            columns = Columns.raw("*, users!author_uid($KEY_UID, $KEY_USERNAME, $KEY_AVATAR, $KEY_VERIFY)")
        ) {
            filter { eq(KEY_AUTHOR_UID, actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()

        val posts = response.mapNotNull { data -> parsePost(data) }
        val enrichedPosts = populatePostReactions(posts)
        val fullyEnrichedPosts = populatePostPolls(enrichedPosts)
        Result.success(fullyEnrichedPosts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun getMediaItemsByType(userId: String, limit: Int, offset: Int, isVideo: Boolean): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val response = client.from("posts").select(
            columns = Columns.raw("$KEY_ID, $KEY_MEDIA_ITEMS")
        ) {
            filter { eq(KEY_AUTHOR_UID, actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()

        val mediaItems = response.flatMap { data ->
            val postId = data[KEY_ID]?.jsonPrimitive?.contentOrNull ?: return@flatMap emptyList()
            data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap[KEY_URL]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap[KEY_TYPE]?.jsonPrimitive?.contentOrNull ?: MEDIA_TYPE_IMAGE
                val isVideoType = typeStr.equals(MEDIA_TYPE_VIDEO, ignoreCase = true)
                if (isVideoType != isVideo) return@mapNotNull null
                com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem(
                    id = mediaMap[KEY_ID]?.jsonPrimitive?.contentOrNull ?: postId,
                    url = constructMediaUrl(url),
                    isVideo = isVideo
                )
            } ?: emptyList()
        }
        Result.success(mediaItems)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> =
        getMediaItemsByType(userId, limit, offset, isVideo = false)

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> =
        getMediaItemsByType(userId, limit, offset, isVideo = true)

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

    private fun parsePost(data: JsonObject): Post? {
        val post = Post(
            id = data.getNullableString(KEY_ID) ?: return null,
            authorUid = data.getString(KEY_AUTHOR_UID),
            postText = data.getNullableString(KEY_POST_TEXT),
            postImage = data.getNullableString(KEY_POST_IMAGE)?.let { constructMediaUrl(it) },
            postType = data.getNullableString(KEY_POST_TYPE),
            timestamp = data.getLong(KEY_TIMESTAMP),
            publishDate = data.getNullableString("publish_date") ?: data.getNullableString("created_at"),
            likesCount = data.getInt(KEY_LIKES_COUNT),
            commentsCount = data.getInt(KEY_COMMENTS_COUNT),
            viewsCount = data.getInt(KEY_VIEWS_COUNT),
            hasPoll = data.getBoolean(KEY_HAS_POLL),
            pollQuestion = data.getNullableString(KEY_POLL_QUESTION),
            pollOptions = data[KEY_POLL_OPTIONS]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj.getNullableString(KEY_TEXT) ?: return@mapNotNull null
                PollOption(text, obj.getInt(KEY_VOTES))
            }
        )

        data[KEY_USERS]?.jsonObject?.let { userData ->
            post.username = userData.getNullableString(KEY_USERNAME)
            post.displayName = userData.getNullableString("display_name")
            post.avatarUrl = userData.getNullableString(KEY_AVATAR)?.let { constructAvatarUrl(it) }
            post.isVerified = userData.getBoolean(KEY_VERIFY)
        }

        data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.let { mediaData ->
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap.getNullableString(KEY_URL) ?: return@mapNotNull null
                MediaItem(
                    id = mediaMap.getString(KEY_ID),
                    url = constructMediaUrl(url),
                    type = if (mediaMap.getNullableString(KEY_TYPE).equals(MEDIA_TYPE_VIDEO, true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap.getNullableString(KEY_THUMBNAIL_URL)?.let { constructMediaUrl(it) }
                )
            }.toMutableList()
        }

        return post
    }

    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        return reactionRepository.populatePostReactions(posts)
    }

    private suspend fun populatePostPolls(posts: List<Post>): List<Post> {
        val pollPosts = posts.filter { it.hasPoll == true }
        if (pollPosts.isEmpty()) return posts

        val postIds = pollPosts.map { it.id }

        val userVotesResult = pollRepository.getBatchUserVotes(postIds)
        val userVotes = userVotesResult.getOrNull() ?: emptyMap()

        val pollCountsResult = pollRepository.getBatchPollVotes(postIds)
        val pollCounts = pollCountsResult.getOrNull() ?: emptyMap()

        return posts.map { post ->
            if (post.hasPoll == true) {
                val userVote = userVotes[post.id]
                val counts = pollCounts[post.id] ?: emptyMap()

                val updatedOptions = post.pollOptions?.mapIndexed { index, option ->
                    option.copy(votes = counts[index] ?: 0)
                }

                val updatedPost = post.copy(
                    pollOptions = updatedOptions
                )
                updatedPost.userPollVote = userVote
                updatedPost
            } else {
                post
            }
        }
    }
}
