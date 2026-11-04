package com.synapse.social.studioasinc.feature.shared.sharepost

import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.shared.domain.model.chat.SharedPostMetadata

/**
 * Builds the chat card payload from a feed post. Media URLs are already signed by the time a post
 * reaches the UI, so the card degrades to text once a link expires.
 */
fun Post.toSharedPostMetadata(): SharedPostMetadata {
    val firstMedia = mediaItems?.firstOrNull()
    return SharedPostMetadata(
        postId = id,
        authorName = displayName ?: username ?: authorUid,
        authorUsername = username,
        authorAvatarUrl = avatarUrl,
        content = postText,
        mediaUrl = firstMedia?.url ?: postImage,
        isVideo = firstMedia?.type == MediaType.VIDEO
    )
}
