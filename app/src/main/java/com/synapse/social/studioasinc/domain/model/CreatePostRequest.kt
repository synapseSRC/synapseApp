package com.synapse.social.studioasinc.domain.model

data class CreatePostRequest(
    val postText: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val privacy: String = "public",
    val pollQuestion: String? = null,
    val pollOptions: List<String>? = null,
    val pollDurationHours: Int = 24,
    val location: LocationData? = null,
    val taggedPeople: List<String> = emptyList(),
    val feeling: String? = null,
    val textBackgroundColor: Long? = null,
    val youtubeUrl: String? = null,
    val hideViewsCount: Boolean = false,
    val hideLikeCount: Boolean = false,
    val hideCommentsCount: Boolean = false,
    val disableComments: Boolean = false,
    val isEditMode: Boolean = false,
    val editPostId: String? = null,
    val replyToPostId: String? = null
)
