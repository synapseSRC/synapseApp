package com.synapse.social.studioasinc.data.repository

import android.net.Uri
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase
import com.synapse.social.studioasinc.shared.domain.model.MediaType

import com.synapse.social.studioasinc.data.model.StoryCreateRequest
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.StoryPrivacy
import com.synapse.social.studioasinc.domain.model.StoryReaction
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



    suspend fun reactToStory(storyId: String, userId: String, emoji: String): Result<Unit>



    suspend fun removeReaction(storyId: String, userId: String): Result<Unit>



    suspend fun getReactions(storyId: String): Result<List<StoryReaction>>
}

class StoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val uploadMediaUseCase: UploadMediaUseCase
) : StoryRepository {
    private val client = SupabaseClient.client


    companion object {
        private const val TABLE_STORIES = "stories"
        private const val TABLE_STORY_VIEWS = "story_views"
        private const val TABLE_STORY_REACTIONS = "story_reactions"
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
                .decodeList<StoryWithUserDto>()


            val storiesByUser = mutableMapOf<String, MutableList<Story>>()
            val usersMap = mutableMapOf<String, User>()

            for (dto in stories) {
                val story = dto.toDomain()
                storiesByUser.getOrPut(dto.userId) { mutableListOf() }.add(story)

                if (!usersMap.containsKey(dto.userId)) {
                    dto.user?.let {
                        usersMap[dto.userId] = it.toDomain(dto.userId)
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


            val seenStoryIds = if (stories.isEmpty()) {
                emptySet()
            } else {
                client.from(TABLE_STORY_VIEWS)
                    .select(columns = Columns.raw("story_id")) {
                        filter {
                            eq("viewer_id", currentUserId)
                            isIn("story_id", stories.mapNotNull { it.id })
                        }
                    }
                    .decodeList<JsonObject>()
                    .mapNotNull { it["story_id"]?.jsonPrimitive?.content }
                    .toSet()
            }

            for ((userId, userStories) in storiesByUser) {
                if (userId == currentUserId) continue
                usersMap[userId]?.let { user ->
                    val sortedStories = userStories.sortedByDescending { it.createdAt }
                    val latestStoryId = sortedStories.firstOrNull()?.id
                    val hasUnseen = latestStoryId != null && !seenStoryIds.contains(latestStoryId)

                    result.add(
                        StoryWithUser(
                            user = user,
                            stories = sortedStories,
                            hasUnseenStories = hasUnseen,
                            latestStoryTime = sortedStories.maxOfOrNull { it.createdAt ?: "" }
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
            .decodeList<StoryViewWithUserDto>()

        val result = views.map { it.toDomain() }

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

    override suspend fun reactToStory(storyId: String, userId: String, emoji: String): Result<Unit> = try {
        val reaction = buildJsonObject {
            put("story_id", storyId)
            put("user_id", userId)
            put("emoji", emoji)
        }

        client.from(TABLE_STORY_REACTIONS).upsert(reaction) {
            onConflict = "story_id,user_id"
        }

        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeReaction(storyId: String, userId: String): Result<Unit> = try {
        client.from(TABLE_STORY_REACTIONS).delete {
            filter {
                eq("story_id", storyId)
                eq("user_id", userId)
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getReactions(storyId: String): Result<List<StoryReaction>> = try {
        val reactions = client.from(TABLE_STORY_REACTIONS)
            .select {
                filter {
                    eq("story_id", storyId)
                }
            }
            .decodeList<StoryReaction>()
        Result.success(reactions)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
