package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import com.synapse.social.studioasinc.data.repository.ProfileRepositoryImpl.Companion.KEY_UID
import com.synapse.social.studioasinc.data.repository.ProfileRepositoryImpl.Companion.KEY_USERNAME
import com.synapse.social.studioasinc.data.repository.ProfileRepositoryImpl.Companion.KEY_AVATAR
import com.synapse.social.studioasinc.data.repository.ProfileRepositoryImpl.Companion.KEY_VERIFY

internal class ProfilePostsRepository(
    private val client: SupabaseClient,
    private val commentRepository: CommentRepository,
    private val constructMediaUrl: (String) -> String,
    private val constructAvatarUrl: (String) -> String,
    private val resolveUserId: (String) -> String?
) {

    private val reactionRepository = ReactionRepository(client)
    private val pollRepository = PollRepository(client)

    companion object {
        internal const val KEY_ID = "id"
        internal const val KEY_AUTHOR_UID = "author_uid"
        internal const val KEY_POST_TEXT = "post_text"
        internal const val KEY_POST_IMAGE = "post_image"
        internal const val KEY_POST_TYPE = "post_type"
        internal const val KEY_TIMESTAMP = "timestamp"
        internal const val KEY_LIKES_COUNT = "likes_count"
        internal const val KEY_COMMENTS_COUNT = "comments_count"
        internal const val KEY_VIEWS_COUNT = "views_count"
        internal const val KEY_HAS_POLL = "has_poll"
        internal const val KEY_POLL_QUESTION = "poll_question"
        internal const val KEY_POLL_OPTIONS = "poll_options"
        internal const val KEY_MEDIA_ITEMS = "media_items"
        internal const val KEY_USERS = "users"
        internal const val KEY_URL = "url"
        internal const val KEY_TYPE = "type"
        internal const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        internal const val KEY_TEXT = "text"
        internal const val KEY_VOTES = "votes"

        internal const val MEDIA_TYPE_VIDEO = "VIDEO"
        internal const val MEDIA_TYPE_IMAGE = "IMAGE"
    }

    suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<Post>> = try {
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

    internal suspend fun getMediaItemsByType(userId: String, limit: Int, offset: Int, isVideo: Boolean): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        val response = client.from("posts").select(
            columns = Columns.raw("$KEY_ID, $KEY_MEDIA_ITEMS")
        ) {
            filter { eq(KEY_AUTHOR_UID, actualUserId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()

        val mediaItems = response.flatMap { data ->
            val postId = data[KEY_ID]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return@flatMap emptyList()
            data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap[KEY_URL]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap[KEY_TYPE]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: MEDIA_TYPE_IMAGE
                val isVideoType = typeStr.equals(MEDIA_TYPE_VIDEO, ignoreCase = true)
                if (isVideoType != isVideo) return@mapNotNull null
                com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem(
                    id = mediaMap[KEY_ID]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: postId,
                    url = constructMediaUrl(url),
                    isVideo = isVideo
                )
            } ?: emptyList()
        }
        Result.success(mediaItems)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> =
        getMediaItemsByType(userId, limit, offset, isVideo = false)

    suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<com.synapse.social.studioasinc.feature.profile.profile.components.MediaItem>> =
        getMediaItemsByType(userId, limit, offset, isVideo = true)

    suspend fun getProfileReplies(userId: String, limit: Int, offset: Int): Result<List<CommentWithUser>> = try {
        val actualUserId = resolveUserId(userId) ?: return Result.failure(Exception("User not authenticated"))
        commentRepository.fetchUserComments(actualUserId, limit, offset)
    } catch (e: Exception) {
        Result.failure(e)
    }

    internal fun parsePost(data: JsonObject): Post? {
        val post = Post(
            id = data.getNullableString(KEY_ID) ?: return null,
            authorUid = data.getString(KEY_AUTHOR_UID),
            postText = data.getNullableString(KEY_POST_TEXT),
            postImage = data.getNullableString(KEY_POST_IMAGE)?.let { constructMediaUrl(it) },
            postType = data.getNullableString(KEY_POST_TYPE),
            timestamp = data.getLong(KEY_TIMESTAMP),
            publishDate = data.getNullableString("publish_date") ?: data.getNullableString("created_at"),
            likesCount = data.getInt(KEY_LIKES_COUNT),
            replyCount = data.getInt(KEY_COMMENTS_COUNT),
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

    internal suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        return reactionRepository.populatePostReactions(posts)
    }

    internal suspend fun populatePostPolls(posts: List<Post>): List<Post> {
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
