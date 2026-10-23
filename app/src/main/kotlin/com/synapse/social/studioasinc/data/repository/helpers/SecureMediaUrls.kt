package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient

/** Converts stored Storage keys and legacy URL strings to short-lived, RLS-authorized URLs. */
internal suspend fun Post.withSignedStorageMediaUrls(): Post {
    android.util.Log.d("MediaDebug", "signing post=${id} postImage=$postImage mediaCount=${mediaItems?.size}")
    val signedItems = mediaItems?.map { media ->
        val signed = SupabaseClient.createSignedStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, media.url)
        android.util.Log.d("MediaDebug", "  item in=${media.url} out=$signed")
        media.copy(
            url = signed,
            thumbnailUrl = media.thumbnailUrl?.let {
                SupabaseClient.createSignedStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, it)
            }
        )
    }?.toMutableList()
    val signedPostImage = postImage?.let { SupabaseClient.createSignedStorageUrl(SupabaseClient.BUCKET_POST_MEDIA, it) }
    android.util.Log.d("MediaDebug", "  postImage out=$signedPostImage")
    return copy(
        postImage = signedPostImage,
        mediaItems = signedItems,
        quotedPost = quotedPost?.withSignedStorageMediaUrls(),
        inReplyToPost = inReplyToPost?.withSignedStorageMediaUrls()
    )
}

internal suspend fun Story.withSignedStorageMediaUrls(defaultBucket: String = "story-media"): Story = copy(
    mediaUrl = mediaUrl?.let { SupabaseClient.createSignedStorageUrl(defaultBucket, it) },
    thumbnailUrl = thumbnailUrl?.let { SupabaseClient.createSignedStorageUrl(defaultBucket, it) }
)
