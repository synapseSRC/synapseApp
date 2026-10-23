package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.shared.components.post.MediaContent

@Composable
fun MediaPager(
    mediaUrls: List<String>,
    isVideo: Boolean,
    onMediaClick: (Post, Int) -> Unit,
    modifier: Modifier = Modifier,
    post: Post? = null
) {
    MediaContent(
        mediaUrls = mediaUrls,
        isVideo = isVideo,
        onMediaClick = onMediaClick,
        modifier = modifier,
        post = post
    )
}
