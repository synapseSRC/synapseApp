package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.shared.components.post.MediaContent

@Composable
fun MediaPager(
    mediaUrls: List<String>,
    isVideo: Boolean,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MediaContent(
        mediaUrls = mediaUrls,
        isVideo = isVideo,
        onMediaClick = onMediaClick,
        modifier = modifier
    )
}
