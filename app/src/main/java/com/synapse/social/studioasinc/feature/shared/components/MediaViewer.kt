package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun MediaViewer(
    mediaUrls: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { mediaUrls.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Intentional: full-screen media viewer requires black background
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = mediaUrls[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }


        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.Medium)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (mediaUrls.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${mediaUrls.size}",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
            )
        }
    }
}
