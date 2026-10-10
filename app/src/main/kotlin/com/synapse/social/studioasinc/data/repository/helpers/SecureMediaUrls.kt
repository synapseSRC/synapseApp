package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient

/** Converts stored Storage keys and legacy URL strings to short-lived, RLS-authorized URLs. */
internal suspend fun Post.withSignedStorageMediaUrls(): Post {
    val signedItems = mediaItems?.map { media ->
        media.copy(
            url = SupabaseClient.createSignedStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, media.url),
            thumbnailUrl = media.thumbnailUrl?.let {
                SupabaseClient.createSignedStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, it)
            }
        )
    }?.toMutableList()
    return copy(
        postImage = postImage?.let { SupabaseClient.createSignedStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, it) },
        mediaItems = signedItems,
        quotedPost = quotedPost?.withSignedStorageMediaUrls(),
        inReplyToPost = inReplyToPost?.withSignedStorageMediaUrls()
    )
}

internal suspend fun Story.withSignedStorageMediaUrls(defaultBucket: String = "story-media"): Story = copy(
    mediaUrl = mediaUrl?.let { SupabaseClient.createSignedStorageUrl(defaultBucket, it) },
    thumbnailUrl = thumbnailUrl?.let { SupabaseClient.createSignedStorageUrl(defaultBucket, it) }
)
