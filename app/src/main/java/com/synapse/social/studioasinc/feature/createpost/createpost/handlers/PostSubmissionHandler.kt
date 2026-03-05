package com.synapse.social.studioasinc.feature.createpost.createpost.handlers

import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.PostMetadata
import com.synapse.social.studioasinc.ui.createpost.CreatePostUiState
import com.synapse.social.studioasinc.ui.createpost.PollData
import com.synapse.social.studioasinc.ui.createpost.PostSettings
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class PostSubmissionHandler @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val mediaUploadHandler: MediaUploadHandler,
    private val reelSubmissionHandler: ReelSubmissionHandler
) {

    suspend fun submitPost(
        state: CreatePostUiState,
        currentUserId: String,
        originalPost: Post?,
        editPostId: String?,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Verify ownership if editing
            if (originalPost != null && originalPost.authorUid != currentUserId) {
                return@withContext Result.failure(Exception("Unauthorized: You can only edit your own posts"))
            }

            val text = state.postText.trim()
            val hasVideo = state.mediaItems.any { it.type == MediaType.VIDEO }

            if (hasVideo) {
                return@withContext submitReel(state, onProgress)
            }

            if (text.isEmpty() && state.mediaItems.isEmpty() && state.pollData == null && state.youtubeUrl == null) {
                return@withContext Result.failure(Exception("Please add some content to your post"))
            }

            onProgress(0f)

            val postKey = originalPost?.key ?: "post_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val timestamp = System.currentTimeMillis()
            val publishDate = Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val postType = when {
                state.mediaItems.isNotEmpty() -> "IMAGE"
                state.pollData != null -> "POLL"
                else -> "TEXT"
            }

            val pollEndTime = state.pollData?.let {
                Instant.ofEpochMilli(timestamp + it.durationHours * 3600 * 1000L)
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT)
            }

            val post = Post(
                id = editPostId ?: UUID.randomUUID().toString(),
                key = postKey,
                authorUid = currentUserId,
                postText = text.ifEmpty { null },
                postType = postType,
                postVisibility = state.privacy,
                postHideViewsCount = if (state.settings.hideViewsCount) "true" else "false",
                postHideLikeCount = if (state.settings.hideLikeCount) "true" else "false",
                postHideCommentsCount = if (state.settings.hideCommentsCount) "true" else "false",
                postDisableComments = if (state.settings.disableComments) "true" else "false",
                publishDate = publishDate,
                timestamp = timestamp,
                youtubeUrl = state.youtubeUrl,
                hasPoll = state.pollData != null,
                pollQuestion = state.pollData?.question,
                pollOptions = state.pollData?.options?.map { PollOption(text = it, votes = 0) },
                pollEndTime = pollEndTime,
                pollAllowMultiple = false,
                hasLocation = state.location != null,
                locationName = state.location?.name,
                locationAddress = state.location?.address,
                locationLatitude = state.location?.latitude,
                locationLongitude = state.location?.longitude,
                locationPlaceId = null,
                metadata = PostMetadata(
                    layoutType = DEFAULT_LAYOUT_TYPE,
                    taggedPeople = state.taggedPeople.ifEmpty { null },
                    feeling = state.feeling,
                    backgroundColor = state.textBackgroundColor
                )
            )

            val newMedia = state.mediaItems.filter { !it.url.startsWith("http") }
            val existingMedia = state.mediaItems.filter { it.url.startsWith("http") }

            return@withContext if (newMedia.isEmpty()) {
                 val finalPost = post.copy(
                     mediaItems = existingMedia.toMutableList(),
                     postImage = existingMedia.firstOrNull { it.type == MediaType.IMAGE }?.url
                 )
                 saveOrUpdatePost(state, finalPost)
            } else {
                 uploadMediaAndSave(state, post, newMedia, existingMedia, onProgress)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    private suspend fun submitReel(
        state: CreatePostUiState,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        val videoItem = state.mediaItems.firstOrNull { it.type == MediaType.VIDEO }
            ?: return Result.failure(Exception("No video found for reel"))
        val postText = state.postText

        return reelSubmissionHandler.submitReel(
            videoItem = videoItem,
            postText = postText,
            location = state.location,
            taggedPeople = state.taggedPeople,
            feeling = state.feeling,
            textBackgroundColor = state.textBackgroundColor,
            onProgress = onProgress
        )
    }

    private suspend fun uploadMediaAndSave(
        state: CreatePostUiState,
        post: Post,
        newMedia: List<MediaItem>,
        existingMedia: List<MediaItem>,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        return try {
            val uploadedItems = mediaUploadHandler.uploadMedia(newMedia, onProgress)

            val allMedia = existingMedia + uploadedItems
            val updatedPost = post.copy(
                mediaItems = allMedia.toMutableList(),
                postImage = allMedia.firstOrNull { it.type == MediaType.IMAGE }?.url
            )
            saveOrUpdatePost(state, updatedPost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveOrUpdatePost(state: CreatePostUiState, post: Post): Result<Unit> {
        if (!state.isEditMode && post.username.isNullOrEmpty()) {
            try {
                userRepository.getUserById(post.authorUid).getOrThrow()?.let { user ->
                    post.username = user.username
                    post.avatarUrl = user.avatar
                    post.isVerified = user.verify
                }
            } catch (e: Exception) {
                // If user fetch fails, we should fail the submission to prevent data inconsistency
                return Result.failure(Exception("Failed to fetch user details for post creation: ${e.message}", e))
            }
        }

        val result = if (state.isEditMode) {
             postRepository.updatePost(post)
        } else {
             postRepository.createPost(post)
        }

        result.onSuccess { updatedPost ->
            if (state.isEditMode) {
                PostEventBus.emit(PostEvent.Updated(updatedPost))
            } else {
                PostEventBus.emit(PostEvent.Created(updatedPost))
            }
        }

        return result.map { }
    }

    companion object {
        private const val DEFAULT_LAYOUT_TYPE = "COLUMNS"
    }
}
