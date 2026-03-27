package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.domain.model.LinkPreview
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.PollOption
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.PostMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostInsertDto(
    val id: String,
    val key: String? = null,
    @SerialName("author_uid") val authorUid: String,
    @SerialName("post_text") val postText: String? = null,
    @SerialName("post_image") val postImage: String? = null,
    @SerialName("post_type") val postType: String? = null,
    @SerialName("post_visibility") val postVisibility: String? = null,
    @SerialName("post_hide_views_count") val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count") val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count") val postHideReplyCount: String? = null,
    @SerialName("post_disable_comments") val postDisableReplies: String? = null,
    @SerialName("publish_date") val publishDate: String? = null,
    val timestamp: Long,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val replyCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("reshares_count") val resharesCount: Int = 0,
    @SerialName("quoted_post_id") val quotedPostId: String? = null,
    @SerialName("is_quote") val isQuote: Boolean = false,
    @SerialName("media_items") val mediaItems: List<MediaItem>? = null,
    @SerialName("has_poll") val hasPoll: Boolean? = null,
    @SerialName("poll_question") val pollQuestion: String? = null,
    @SerialName("poll_options") val pollOptions: List<PollOption>? = null,
    @SerialName("poll_end_time") val pollEndTime: String? = null,
    @SerialName("poll_allow_multiple") val pollAllowMultiple: Boolean? = null,
    @SerialName("has_location") val hasLocation: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    @SerialName("location_place_id") val locationPlaceId: String? = null,
    @SerialName("youtube_url") val youtubeUrl: String? = null,
    @SerialName("link_previews") val linkPreviews: List<LinkPreview>? = null,
    @SerialName("metadata") val metadata: PostMetadata? = null
)

@Serializable
data class UserSummaryDto(
    val uid: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar") val avatarUrl: String? = null,
    @SerialName("verify") val isVerified: Boolean? = false
)

@Serializable
data class CommentUserDto(
    val username: String? = null
)

@Serializable
data class CommentSelectDto(
    val id: String,
    @SerialName("content") val comment: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("users") val user: CommentUserDto? = null
)

@Serializable
data class PostSelectDto(
    val id: String,
    val key: String? = null,
    @SerialName("author_uid") val authorUid: String,
    @SerialName("post_text") val postText: String? = null,
    @SerialName("post_image") val postImage: String? = null,
    @SerialName("post_type") val postType: String? = null,
    @SerialName("post_visibility") val postVisibility: String? = null,
    @SerialName("post_hide_views_count") val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count") val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count") val postHideReplyCount: String? = null,
    @SerialName("post_disable_comments") val postDisableReplies: String? = null,
    @SerialName("publish_date") val publishDate: String? = null,
    val timestamp: Long,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val replyCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("reshares_count") val resharesCount: Int = 0,
    @SerialName("media_items") val mediaItems: List<MediaItem>? = null,
    @SerialName("has_poll") val hasPoll: Boolean? = null,
    @SerialName("poll_question") val pollQuestion: String? = null,
    @SerialName("poll_options") val pollOptions: List<PollOption>? = null,
    @SerialName("poll_end_time") val pollEndTime: String? = null,
    @SerialName("poll_allow_multiple") val pollAllowMultiple: Boolean? = null,
    @SerialName("has_location") val hasLocation: Boolean? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("location_latitude") val locationLatitude: Double? = null,
    @SerialName("location_longitude") val locationLongitude: Double? = null,
    @SerialName("location_place_id") val locationPlaceId: String? = null,
    @SerialName("youtube_url") val youtubeUrl: String? = null,
    @SerialName("link_previews") val linkPreviews: List<LinkPreview>? = null,
    @SerialName("metadata") val metadata: PostMetadata? = null,
    @SerialName("quoted_post") val quotedPost: PostSelectDto? = null,
    @SerialName("quoted_post_id") val quotedPostId: String? = null,
    @SerialName("is_quote") val isQuote: Boolean = false,


    @SerialName("users") val user: UserSummaryDto? = null,


    @SerialName("latest_comments") val comments: List<CommentSelectDto>? = null,
    
    @SerialName("created_at") val createdAt: String? = null
)

fun Post.toInsertDto(): PostInsertDto {
    return PostInsertDto(
        id = this.id,
        key = this.key,
        authorUid = this.authorUid,
        postText = this.postText,
        postImage = this.postImage,
        postType = this.postType,
        postVisibility = this.postVisibility,
        postHideViewsCount = this.postHideViewsCount,
        postHideLikeCount = this.postHideLikeCount,
        postHideReplyCount = this.postHideReplyCount,
        postDisableReplies = this.postDisableReplies,
        publishDate = this.publishDate,
        timestamp = this.timestamp,
        likesCount = this.likesCount,
        replyCount = this.replyCount,
        viewsCount = this.viewsCount,
        resharesCount = this.resharesCount,
        quotedPostId = this.quotedPostId,
        isQuote = this.isQuote,
        mediaItems = this.mediaItems,
        hasPoll = this.hasPoll,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndTime = this.pollEndTime,
        pollAllowMultiple = this.pollAllowMultiple,
        hasLocation = this.hasLocation,
        locationName = this.locationName,
        locationAddress = this.locationAddress,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationPlaceId = this.locationPlaceId,
        youtubeUrl = this.youtubeUrl,
        linkPreviews = this.linkPreviews,
        metadata = this.metadata
    )
}

fun Post.toUpdateDto(): PostInsertDto {

    return PostInsertDto(
        id = this.id,
        key = this.key,
        authorUid = this.authorUid,
        postText = this.postText,
        postImage = this.postImage,
        postType = this.postType,
        postVisibility = this.postVisibility,
        postHideViewsCount = this.postHideViewsCount,
        postHideLikeCount = this.postHideLikeCount,
        postHideReplyCount = this.postHideReplyCount,
        postDisableReplies = this.postDisableReplies,
        publishDate = this.publishDate,
        timestamp = this.timestamp,
        likesCount = this.likesCount,
        replyCount = this.replyCount,
        viewsCount = this.viewsCount,
        resharesCount = this.resharesCount,
        quotedPostId = this.quotedPostId,
        isQuote = this.isQuote,
        mediaItems = this.mediaItems,
        hasPoll = this.hasPoll,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndTime = this.pollEndTime,
        pollAllowMultiple = this.pollAllowMultiple,
        hasLocation = this.hasLocation,
        locationName = this.locationName,
        locationAddress = this.locationAddress,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationPlaceId = this.locationPlaceId,
        youtubeUrl = this.youtubeUrl,
        linkPreviews = this.linkPreviews,
        metadata = this.metadata
    )
}

fun PostSelectDto.toDomain(constructMediaUrl: (String) -> String, constructAvatarUrl: (String) -> String): Post {
    val post = Post(
        id = this.id,
        key = this.key,
        authorUid = this.authorUid,
        postText = this.postText,
        postImage = this.postImage?.let { if (it.startsWith("http")) it else constructMediaUrl(it) },
        postType = this.postType,
        postHideViewsCount = this.postHideViewsCount,
        postHideLikeCount = this.postHideLikeCount,
        postHideReplyCount = this.postHideReplyCount,
        postDisableReplies = this.postDisableReplies,
        postVisibility = this.postVisibility,
        publishDate = this.publishDate,
        createdAt = this.createdAt,
        timestamp = this.timestamp,
        likesCount = this.likesCount,
        replyCount = this.replyCount,
        viewsCount = this.viewsCount,
        resharesCount = this.resharesCount,
        mediaItems = this.mediaItems?.map {
            it.copy(
                url = if (it.url.startsWith("http")) it.url else constructMediaUrl(it.url),
                thumbnailUrl = it.thumbnailUrl?.let { thumb -> if (thumb.startsWith("http")) thumb else constructMediaUrl(thumb) }
            )
        }?.toMutableList(),
        hasPoll = this.hasPoll,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndTime = this.pollEndTime,
        pollAllowMultiple = this.pollAllowMultiple,
        hasLocation = this.hasLocation,
        locationName = this.locationName,
        locationAddress = this.locationAddress,
        locationLatitude = this.locationLatitude,
        locationLongitude = this.locationLongitude,
        locationPlaceId = this.locationPlaceId,
        youtubeUrl = this.youtubeUrl,
        linkPreviews = this.linkPreviews,
        metadata = this.metadata,
        quotedPostId = this.quotedPostId,
        isQuote = this.isQuote,
        quotedPost = this.quotedPost?.toDomain(constructMediaUrl, constructAvatarUrl)
    )


    this.user?.let { u ->
        post.username = u.username
        post.displayName = u.displayName
        post.avatarUrl = u.avatarUrl?.let { if (it.startsWith("http")) it else constructAvatarUrl(it) }
        post.isVerified = u.isVerified ?: false
    }


    this.comments?.sortedByDescending { it.createdAt }?.firstOrNull()?.let { comment ->
        post.latestCommentText = comment.comment
        post.latestCommentAuthor = comment.user?.username
    }

    return post
}
