package com.synapse.social.studioasinc.data.repository

import android.net.Uri
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import com.synapse.social.studioasinc.shared.domain.model.MediaType

import com.synapse.social.studioasinc.data.model.StoryCreateRequest
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.domain.model.StoryView
import com.synapse.social.studioasinc.domain.model.StoryViewWithUser
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.core.util.UriUtils
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CancellationException

interface StoryRepository {


    suspend fun hasActiveStory(userId: String): Result<Boolean>



    fun getActiveStories(currentUserId: String): Flow<List<StoryWithUser>>



    suspend fun getUserStories(userId: String): Result<List<Story>>



    suspend fun createStory(
        userId: String,
        mediaUri: Uri,
        mediaType: StoryMediaType,
        privacy: StoryPrivacy,
        duration: Int = 5
    ): Result<Story>



    suspend fun deleteStory(storyId: String): Result<Unit>



    suspend fun markAsSeen(storyId: String, viewerId: String): Result<Unit>



    suspend fun getStoryViewers(storyId: String): Result<List<StoryViewWithUser>>



    suspend fun hasSeenStory(storyId: String, viewerId: String): Result<Boolean>
}

class StoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val uploadMediaUseCase: UploadMediaUseCase
) : StoryRepository {
    private val client = SupabaseClient.client


    companion object {
        private const val TABLE_STORIES = "stories"
        private const val TABLE_STORY_VIEWS = "story_views"
        private const val TABLE_USERS = "users"
    }

    override suspend fun hasActiveStory(userId: String): Result<Boolean> = try {
        val now = Instant.now().toString()

        val count = client.from(TABLE_STORIES).select {
            filter {
                eq("user_id", userId)
                gt("expires_at", now)
            }
            count(io.github.jan.supabase.postgrest.query.Count.EXACT)
        }.countOrNull() ?: 0

        Result.success(count > 0)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getActiveStories(currentUserId: String): Flow<List<StoryWithUser>> = flow {
        try {
            val now = Instant.now().toString()


            val followingList = try {
                client.from("follows")
                    .select(columns = Columns.raw("following_id")) {
                        filter {
                            eq("follower_id", currentUserId)
                        }
                    }
                    .decodeList<JsonObject>()
                    .mapNotNull { it["following_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content }
                    .toMutableList()
            } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
                mutableListOf<String>()
            }


            if (!followingList.contains(currentUserId)) {
                followingList.add(currentUserId)
            }


            val stories = client.from(TABLE_STORIES)
                .select(columns = Columns.raw("*, users:users!user_id(*)")) {
                    filter {
                        gt("expires_at", now)
                        isIn("user_id", followingList)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JsonObject>()


            val storiesByUser = mutableMapOf<String, MutableList<Story>>()
            val usersMap = mutableMapOf<String, User>()

            for (storyJson in stories) {
                val userId = storyJson["user_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content ?: continue

                val story = Story(
                    id = storyJson["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    userId = userId,
                    mediaUrl = storyJson["media_url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    mediaType = try {
                        storyJson["media_type"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.let {
                            StoryMediaType.valueOf(it.uppercase())
                        }
                    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { null },
                    content = storyJson["content"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    duration = storyJson["duration"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    durationHours = storyJson["duration_hours"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    privacy = try {
                        storyJson["privacy_setting"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.let {
                            when(it) {
                                "followers" -> StoryPrivacy.FOLLOWERS
                                "public" -> StoryPrivacy.PUBLIC
                                else -> null
                            }
                        }
                    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) { null },
                    viewCount = storyJson["views_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    isActive = storyJson["is_active"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toBooleanStrictOrNull(),
                    thumbnailUrl = storyJson["thumbnail_url"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    mediaWidth = storyJson["media_width"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    mediaHeight = storyJson["media_height"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    mediaDurationSeconds = storyJson["media_duration_seconds"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    fileSizeBytes = storyJson["file_size_bytes"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toLongOrNull(),
                    reactionsCount = storyJson["reactions_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    repliesCount = storyJson["replies_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toIntOrNull(),
                    isReported = storyJson["is_reported"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toBooleanStrictOrNull(),
                    moderationStatus = storyJson["moderation_status"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    createdAt = storyJson["created_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    expiresAt = storyJson["expires_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content
                )

                storiesByUser.getOrPut(userId) { mutableListOf() }.add(story)


                if (!usersMap.containsKey(userId)) {
                    val userJson = storyJson["users"] as? JsonObject
                    if (userJson != null) {
                        val avatarPath = userJson["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content
                        val avatarUrl = avatarPath?.let { path ->
                            if (path.startsWith("http")) path else SupabaseClient.constructAvatarUrl(path)
                        }
                        usersMap[userId] = User(
                            id = userJson["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                            uid = userJson["uid"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content ?: userId,
                            username = userJson["username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                            displayName = userJson["display_name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                            avatar = avatarUrl,
                            verify = userJson["verify"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content?.toBooleanStrictOrNull() ?: false
                        )
                    }
                }
            }


            val result = mutableListOf<StoryWithUser>()


            storiesByUser[currentUserId]?.let { userStories ->
                usersMap[currentUserId]?.let { user ->
                    result.add(
                        StoryWithUser(
                            user = user,
                            stories = userStories.sortedByDescending { it.createdAt },
                            hasUnseenStories = false,
                            latestStoryTime = userStories.maxOfOrNull { it.createdAt ?: "" }
                        )
                    )
                }
            }


            for ((userId, userStories) in storiesByUser) {
                if (userId == currentUserId) continue
                usersMap[userId]?.let { user ->
                    result.add(
                        StoryWithUser(
                            user = user,
                            stories = userStories.sortedByDescending { it.createdAt },
                            hasUnseenStories = true,
                            latestStoryTime = userStories.maxOfOrNull { it.createdAt ?: "" }
                        )
                    )
                }
            }

            emit(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("StoryRepository", "Error fetching active stories", e)
            emit(emptyList())
        }
    }

    override suspend fun getUserStories(userId: String): Result<List<Story>> = try {
        val now = Instant.now().toString()

        val stories = client.from(TABLE_STORIES)
            .select(columns = Columns.raw("*")) {
                filter {
                    eq("user_id", userId)
                    gt("expires_at", now)
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<Story>()

        Result.success(stories)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createStory(
        userId: String,
        mediaUri: Uri,
        mediaType: StoryMediaType,
        privacy: StoryPrivacy,
        duration: Int
    ): Result<Story> = try {

        val filePath = UriUtils.getPathFromUri(context, mediaUri)
            ?: throw Exception("Could not convert URI to file path")


        val mediaUrl = try {
            val sharedMediaType = when (mediaType) {
                StoryMediaType.PHOTO -> MediaType.PHOTO
                StoryMediaType.VIDEO -> MediaType.VIDEO
            }

            val result = uploadMediaUseCase(
                filePath = filePath,
                mediaType = sharedMediaType,
                onProgress = {}
            )
            result.getOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: throw Exception("Media upload failed")


        val now = Instant.now()
        val expiresAt = now.plusSeconds(24 * 60 * 60)


        val storyData = StoryCreateRequest(
            userId = userId,
            mediaUrl = mediaUrl,
            mediaType = when (mediaType) {
                StoryMediaType.PHOTO -> "photo"
                StoryMediaType.VIDEO -> "video"
            },
            privacySetting = when (privacy) {
                StoryPrivacy.ALL_FRIENDS -> "followers"
                StoryPrivacy.FOLLOWERS -> "followers"
                StoryPrivacy.PUBLIC -> "public"
            },
            duration = if (mediaType == StoryMediaType.VIDEO) duration else null,
            durationHours = 24,
            mediaDurationSeconds = if (mediaType == StoryMediaType.VIDEO) duration else null,
            isActive = true,
            reactionsCount = 0,
            repliesCount = 0,
            isReported = false,
            moderationStatus = "pending"
        )

        val insertedStory = client.from(TABLE_STORIES)
            .insert(storyData) {
                select()
            }
            .decodeSingle<Story>()

        Result.success(insertedStory)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteStory(storyId: String): Result<Unit> = try {
        client.from(TABLE_STORIES)
            .delete {
                filter {
                    eq("id", storyId)
                }
            }
        Result.success(Unit)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsSeen(storyId: String, viewerId: String): Result<Unit> = try {
        // Use buildJsonObject to avoid sending explicit null for viewed_at
        val view = buildJsonObject {
            put("story_id", storyId)
            put("viewer_id", viewerId)
        }

        client.from(TABLE_STORY_VIEWS).upsert(view) {
            onConflict = "story_id,viewer_id"
            ignoreDuplicates = true
        }

        Result.success(Unit)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStoryViewers(storyId: String): Result<List<StoryViewWithUser>> = try {
        val views = client.from(TABLE_STORY_VIEWS)
            .select(columns = Columns.raw("*, users!viewer_id(*)")) {
                filter {
                    eq("story_id", storyId)
                }
                order("viewed_at", Order.DESCENDING)
            }
            .decodeList<JsonObject>()

        val result = views.mapNotNull { viewJson ->
            val storyView = StoryView(
                id = viewJson["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                storyId = viewJson["story_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content ?: return@mapNotNull null,
                viewerId = viewJson["viewer_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content ?: return@mapNotNull null,
                viewedAt = viewJson["viewed_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content
            )

            val userJson = viewJson["users"] as? JsonObject
            val viewer = userJson?.let {
                val avatarPath = it["avatar"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content
                val avatarUrl = avatarPath?.let { path ->
                    if (path.startsWith("http")) path else SupabaseClient.constructAvatarUrl(path)
                }
                User(
                    id = it["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    uid = it["uid"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content ?: storyView.viewerId,
                    username = it["username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    displayName = it["display_name"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.content,
                    avatar = avatarUrl
                )
            }

            StoryViewWithUser(storyView = storyView, viewer = viewer)
        }

        Result.success(result)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun hasSeenStory(storyId: String, viewerId: String): Result<Boolean> = try {
        val count = client.from(TABLE_STORY_VIEWS)
            .select {
                filter {
                    eq("story_id", storyId)
                    eq("viewer_id", viewerId)
                }
                count(io.github.jan.supabase.postgrest.query.Count.EXACT)
            }
            .countOrNull() ?: 0

        Result.success(count > 0)
    } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        Result.failure(e)
    }
}
