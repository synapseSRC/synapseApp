package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient

/** Converts stored Storage keys to viewer-authorized URLs without letting inaccessible media
 * suppress the post that owns it. */
internal suspend fun Post.withSignedStorageMediaUrls(): Post {
    val signedItems = mediaItems?.mapNotNull { media ->
        val signedUrl = SupabaseClient.createSignedStorageUrlOrNull(SupabaseClient.BUCKET_POST_MEDIA, media.url)
            ?: return@mapNotNull null
        media.copy(
            url = signedUrl,
            thumbnailUrl = media.thumbnailUrl?.let {
                SupabaseClient.createSignedStorageUrlOrNull(SupabaseClient.BUCKET_POST_MEDIA, it)
            }
        )
    }?.toMutableList()
    val signedPostImage = postImage?.let {
        SupabaseClient.createSignedStorageUrlOrNull(SupabaseClient.BUCKET_POST_MEDIA, it)
    }
    return copy(
        postImage = signedPostImage,
        mediaItems = signedItems,
        quotedPost = quotedPost?.withSignedStorageMediaUrls(),
        inReplyToPost = inReplyToPost?.withSignedStorageMediaUrls()
    )
}

internal suspend fun Story.withSignedStorageMediaUrls(defaultBucket: String = "story-media"): Story = copy(
    mediaUrl = mediaUrl?.let { SupabaseClient.createSignedStorageUrlOrNull(defaultBucket, it) },
    thumbnailUrl = thumbnailUrl?.let { SupabaseClient.createSignedStorageUrlOrNull(defaultBucket, it) }
)
