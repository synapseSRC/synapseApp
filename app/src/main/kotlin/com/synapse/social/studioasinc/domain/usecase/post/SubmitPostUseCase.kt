package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.data.repository.UserRepositoryImpl
import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.PostMetadata
import com.synapse.social.studioasinc.domain.repository.MediaUploadHandler
import com.synapse.social.studioasinc.domain.repository.PostRepository
import com.synapse.social.studioasinc.domain.repository.ReelSubmissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class SubmitPostUseCase @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepositoryImpl,
    private val mediaUploadHandler: MediaUploadHandler,
    private val reelSubmissionHandler: ReelSubmissionHandler
) {

    suspend operator fun invoke(
        requests: List<CreatePostRequest>,
        currentUserId: String,
        onProgress: (Float) -> Unit
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            if (requests.isEmpty()) return@withContext Result.success(emptyList())

            val totalMediaItems = requests.sumOf { it.mediaItems.size }
            val mediaUploadResults = mutableMapOf<Int, List<MediaItem>>()

            // Step 1: Parallelize media uploads across all requests
            val allMediaResults: List<Pair<Int, List<MediaItem>>> = coroutineScope {
                requests.mapIndexed { requestIndex, request ->
                    async {
                        val newMedia = request.mediaItems.filter { !it.url.startsWith("http") }
                        val existingMedia = request.mediaItems.filter { it.url.startsWith("http") }

                        if (newMedia.isEmpty()) {
                            requestIndex to existingMedia
                        } else {
                            val uploaded = mediaUploadHandler.uploadMedia(newMedia) { progress ->
                                // This is a rough estimation of global progress
                            }
                            requestIndex to (existingMedia + uploaded)
                        }
                    }
                }.awaitAll()
            }
            onProgress(0.5f) // Halfway there after uploads

            // Step 2: Construct all Post objects and maintain reply chain
            val now = Clock.System.now()
            val timestamp = now.toEpochMilliseconds()
            val publishDate = now.toString()

            val postsToCreate = mutableListOf<Post>()
            var previousPostId: String? = requests.first().replyToPostId

            // Pre-fetch user info once
            var username: String? = null
            var avatarUrl: String? = null
            var isVerified = false
            try {
                userRepository.getUserById(currentUserId).getOrThrow()?.let { user ->
                    username = user.username
                    avatarUrl = user.avatar
                    isVerified = user.verify
                }
            } catch (e: Exception) {
                // Ignore user info fetch error, will be handled by repository fallback if needed
            }

            requests.forEachIndexed { index, request ->
                val postId = "${kotlin.random.Random.nextLong().toULong().toString(16)}-${kotlin.random.Random.nextLong().toULong().toString(16)}"
                val mediaItems = allMediaResults.find { it.first == index }?.second ?: emptyList<MediaItem>()

                val postType = when {
                    mediaItems.isNotEmpty() -> "IMAGE"
                    request.pollOptions != null -> "POLL"
                    else -> "TEXT"
                }

                val post = Post(
                    id = postId,
                    key = "post_${timestamp}_${(1000..9999).random()}_$index",
                    authorUid = currentUserId,
                    postText = request.postText.trim().ifEmpty { null },
                    postType = postType,
                    postVisibility = request.privacy,
                    inReplyToPostId = previousPostId,
                    isQuote = false,
                    postHideViewsCount = if (request.hideViewsCount) "true" else "false",
                    postHideLikeCount = if (request.hideLikeCount) "true" else "false",
                    postHideReplyCount = if (request.hideCommentsCount) "true" else "false",
                    postDisableReplies = if (request.disableComments) "true" else "false",
                    publishDate = publishDate,
                    timestamp = timestamp + index, // Slightly increment timestamp to maintain order
                    youtubeUrl = request.youtubeUrl,
                    hasPoll = request.pollOptions != null,
                    pollQuestion = request.pollQuestion,
                    pollOptions = request.pollOptions?.map { PollOption(text = it, votes = 0) },
                    pollEndTime = request.pollOptions?.let {
                        kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp + request.pollDurationHours * 3600 * 1000L).toString()
                    },
                    pollAllowMultiple = false,
                    hasLocation = request.location != null,
                    locationName = request.location?.name,
                    locationAddress = request.location?.address,
                    locationLatitude = request.location?.latitude,
                    locationLongitude = request.location?.longitude,
                    metadata = PostMetadata(
                        layoutType = DEFAULT_LAYOUT_TYPE,
                        taggedPeople = request.taggedPeople.ifEmpty { null }?.map { com.synapse.social.studioasinc.domain.model.User(uid = it) },
                        backgroundColor = request.textBackgroundColor
                    ),
                    mediaItems = mediaItems.toMutableList(),
                    postImage = mediaItems.firstOrNull { it.type == MediaType.IMAGE }?.url,
                    username = username,
                    avatarUrl = avatarUrl,
                    isVerified = isVerified
                )
                postsToCreate.add(post)
                previousPostId = postId
            }

            // Step 3: Batch create posts
            val result = postRepository.createPosts(postsToCreate)
            onProgress(1.0f)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun buildAndSubmitSingle(
        request: CreatePostRequest,
        currentUserId: String,
        originalPost: Post?,
        onProgress: (Float) -> Unit
    ): Result<Post?> = withContext(Dispatchers.IO) {
        try {
            if (originalPost != null && originalPost.authorUid != currentUserId) {
                return@withContext Result.failure(Exception("Unauthorized: You can only edit your own posts"))
            }

            val hasVideo = request.mediaItems.any { it.type == MediaType.VIDEO }
            if (hasVideo) {
                return@withContext reelSubmissionHandler.submitReel(request, onProgress).map { null }
            }

            val text = request.postText.trim()
            if (text.isEmpty() && request.mediaItems.isEmpty() && request.pollOptions == null && request.youtubeUrl == null) {
                return@withContext Result.failure(Exception("Please add some content to your post"))
            }

            onProgress(0f)

            val now = Clock.System.now()
            val timestamp = now.toEpochMilliseconds()
            val publishDate = now.toString()

            val postType = when {
                request.mediaItems.isNotEmpty() -> "IMAGE"
                request.pollOptions != null -> "POLL"
                else -> "TEXT"
            }

            val pollEndTime = request.pollOptions?.let {
                kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp + request.pollDurationHours * 3600 * 1000L).toString()
            }

            val post = Post(
                id = request.editPostId ?: "${kotlin.random.Random.nextLong().toULong().toString(16)}-${kotlin.random.Random.nextLong().toULong().toString(16)}",
                key = originalPost?.key ?: "post_${timestamp}_${(1000..9999).random()}",
                authorUid = currentUserId,
                postText = text.ifEmpty { null },
                postType = postType,
                postVisibility = request.privacy,
                inReplyToPostId = request.replyToPostId,
                isQuote = false,
                postHideViewsCount = if (request.hideViewsCount) "true" else "false",
                postHideLikeCount = if (request.hideLikeCount) "true" else "false",
                postHideReplyCount = if (request.hideCommentsCount) "true" else "false",
                postDisableReplies = if (request.disableComments) "true" else "false",
                publishDate = publishDate,
                timestamp = timestamp,
                youtubeUrl = request.youtubeUrl,
                hasPoll = request.pollOptions != null,
                pollQuestion = request.pollQuestion,
                pollOptions = request.pollOptions?.map { PollOption(text = it, votes = 0) },
                pollEndTime = pollEndTime,
                pollAllowMultiple = false,
                hasLocation = request.location != null,
                locationName = request.location?.name,
                locationAddress = request.location?.address,
                locationLatitude = request.location?.latitude,
                locationLongitude = request.location?.longitude,
                locationPlaceId = null,
                metadata = PostMetadata(
                    layoutType = DEFAULT_LAYOUT_TYPE,
                    taggedPeople = request.taggedPeople.ifEmpty { null }?.map { com.synapse.social.studioasinc.domain.model.User(uid = it) },
                    feeling = null,
                    backgroundColor = request.textBackgroundColor
                )
            )

            val newMedia = request.mediaItems.filter { !it.url.startsWith("http") }
            val existingMedia = request.mediaItems.filter { it.url.startsWith("http") }

            val finalPost = if (newMedia.isEmpty()) {
                post.copy(
                    mediaItems = existingMedia.toMutableList(),
                    postImage = existingMedia.firstOrNull { it.type == MediaType.IMAGE }?.url
                )
            } else {
                val uploaded = mediaUploadHandler.uploadMedia(newMedia, onProgress)
                val allMedia = existingMedia + uploaded
                post.copy(
                    mediaItems = allMedia.toMutableList(),
                    postImage = allMedia.firstOrNull { it.type == MediaType.IMAGE }?.url
                )
            }

            val enrichedPost = enrichWithUserInfo(finalPost, request.isEditMode)

            if (request.isEditMode) {
                postRepository.updatePost(enrichedPost).map { it }
            } else {
                postRepository.createPost(enrichedPost).map { it }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun enrichWithUserInfo(post: Post, isEditMode: Boolean): Post {
        if (!isEditMode && post.username.isNullOrEmpty()) {
            try {
                userRepository.getUserById(post.authorUid).getOrThrow()?.let { user ->
                    post.username = user.username
                    post.avatarUrl = user.avatar
                    post.isVerified = user.verify
                }
            } catch (e: Exception) {
                throw Exception("Failed to fetch user details for post creation: ${e.message}", e)
            }
        }
        return post
    }

    companion object {
        private const val DEFAULT_LAYOUT_TYPE = "COLUMNS"
    }
}
