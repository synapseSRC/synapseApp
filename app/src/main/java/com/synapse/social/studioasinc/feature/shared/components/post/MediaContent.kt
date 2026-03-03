package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import com.synapse.social.studioasinc.ui.settings.PostViewStyle

@Composable
fun MediaContent(
    mediaUrls: List<String>,
    isVideo: Boolean,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE
) {
    if (mediaUrls.isEmpty()) return

    if (mediaUrls.size == 1) {
        val url = mediaUrls.first()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onMediaClick(0) }
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            if (isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Video",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    } else if (postViewStyle == PostViewStyle.GRID) {

        PostMediaGrid(mediaUrls, onMediaClick, modifier)
    } else {

        val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { mediaUrls.size })
        Box(modifier = modifier.fillMaxWidth()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMediaClick(page) }
                        .padding(vertical = 8.dp)
                ) {
                    AsyncImage(
                        model = mediaUrls[page],
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }


            if (mediaUrls.size > 1) {
                androidx.compose.material3.Text(
                    text = "${pagerState.currentPage + 1}/${mediaUrls.size}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun PostMediaGrid(
    mediaUrls: List<String>,
    onMediaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = mediaUrls.size
    val spacing = 2.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        when (count) {
            2 -> {
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp)) {
                    MediaGridItem(url = mediaUrls[0], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(0) })
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(url = mediaUrls[1], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(1) })
                }
            }
            3 -> {

                MediaGridItem(url = mediaUrls[0], modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp), onClick = { onMediaClick(0) })
                Spacer(modifier = Modifier.height(spacing))
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 200.dp)) {
                    MediaGridItem(url = mediaUrls[1], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(1) })
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(url = mediaUrls[2], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(2) })
                }
            }
            else -> {

                Row(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 250.dp)) {
                    MediaGridItem(url = mediaUrls[0], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(0) })
                    Spacer(modifier = Modifier.width(spacing))
                    MediaGridItem(url = mediaUrls[1], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(1) })
                }
                Spacer(modifier = Modifier.height(spacing))
                Row(modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 250.dp)) {
                    MediaGridItem(url = mediaUrls[2], modifier = Modifier.weight(1f).fillMaxSize(), onClick = { onMediaClick(2) })
                    Spacer(modifier = Modifier.width(spacing))

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        MediaGridItem(url = mediaUrls[3], modifier = Modifier.fillMaxSize(), onClick = { onMediaClick(3) })
                        if (count > 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { onMediaClick(3) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${count - 3}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaGridItem(
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AsyncImage(
        model = url,
        contentDescription = "Post Image",
        modifier = modifier
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
