package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.CreatePostRequest
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduledPostDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("post_data") val postData: CreatePostRequestDto,
    @SerialName("scheduled_at") val scheduledAt: String,
    val status: String = "scheduled",
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("published_post_id") val publishedPostId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreatePostRequestDto(
    @SerialName("post_text") val postText: String = "",
    @SerialName("post_image") val postImage: String? = null,
    @SerialName("post_type") val postType: String = "TEXT",
    @SerialName("post_visibility") val postVisibility: String = "public",
    @SerialName("post_hide_views_count") val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count") val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count") val postHideReplyCount: String? = null,
    @SerialName("post_disable_comments") val postDisableReplies: String? = null,
    @SerialName("media_items") val mediaItems: List<MediaItem> = emptyList(),
    @SerialName("has_poll") val hasPoll: Boolean? = null,
    @SerialName("poll_question") val pollQuestion: String? = null,
    @SerialName("poll_options") val pollOptions: List<String>? = null,
    @SerialName("poll_duration_hours") val pollDurationHours: Int = 24,
    @SerialName("has_location") val hasLocation: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    val location: LocationData? = null,
    @SerialName("tagged_people") val taggedPeople: List<String> = emptyList(),
    val feeling: String? = null,
    @SerialName("text_background_color") val textBackgroundColor: Long? = null,
    @SerialName("youtube_url") val youtubeUrl: String? = null,
    @SerialName("in_reply_to_post_id") val replyToPostId: String? = null
)

fun CreatePostRequest.toDto(): CreatePostRequestDto {
    val type = when {
        this.mediaItems.isNotEmpty() -> "IMAGE"
        this.pollOptions != null -> "POLL"
        else -> "TEXT"
    }
    val firstImg = this.mediaItems.firstOrNull { it.type == com.synapse.social.studioasinc.domain.model.MediaType.IMAGE }?.url

    return CreatePostRequestDto(
        postText = this.postText,
        postImage = firstImg,
        postType = type,
        postVisibility = this.privacy,
        postHideViewsCount = if (this.hideViewsCount) "true" else "false",
        postHideLikeCount = if (this.hideLikeCount) "true" else "false",
        postHideReplyCount = if (this.hideCommentsCount) "true" else "false",
        postDisableReplies = if (this.disableComments) "true" else "false",
        mediaItems = this.mediaItems,
        hasPoll = this.pollQuestion != null,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollDurationHours = this.pollDurationHours,
        hasLocation = this.location != null,
        locationName = this.location?.name,
        locationAddress = this.location?.address,
        locationLatitude = this.location?.latitude,
        locationLongitude = this.location?.longitude,
        location = this.location,
        taggedPeople = this.taggedPeople,
        feeling = this.feeling,
        textBackgroundColor = this.textBackgroundColor,
        youtubeUrl = this.youtubeUrl,
        replyToPostId = this.replyToPostId
    )
}

fun CreatePostRequestDto.toDomain(): CreatePostRequest = CreatePostRequest(
    postText = this.postText,
    mediaItems = this.mediaItems,
    privacy = this.postVisibility,
    pollQuestion = this.pollQuestion,
    pollOptions = this.pollOptions,
    pollDurationHours = this.pollDurationHours,
    location = this.location ?: if (this.hasLocation == true && this.locationName != null) LocationData(this.locationName, this.locationAddress, this.locationLatitude, this.locationLongitude) else null,
    taggedPeople = this.taggedPeople,
    feeling = this.feeling,
    textBackgroundColor = this.textBackgroundColor,
    youtubeUrl = this.youtubeUrl,
    hideViewsCount = this.postHideViewsCount == "true",
    hideLikeCount = this.postHideLikeCount == "true",
    hideCommentsCount = this.postHideReplyCount == "true",
    disableComments = this.postDisableReplies == "true",
    replyToPostId = this.replyToPostId
)

fun ScheduledPostDto.toDomain(): ScheduledPost = ScheduledPost(
    id = this.id ?: "",
    userId = this.userId,
    postRequest = this.postData.toDomain(),
    scheduledAt = this.scheduledAt,
    status = this.status,
    errorMessage = this.errorMessage,
    publishedPostId = this.publishedPostId,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
