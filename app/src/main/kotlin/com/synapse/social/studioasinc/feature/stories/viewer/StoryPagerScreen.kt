package com.synapse.social.studioasinc.feature.stories.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun StoryPagerScreen(
    userIds: List<String>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { userIds.size }
    )
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            val userId = userIds[pageIndex]

            // Each page has its own ViewModel to manage that user's stories
            // We use the userId as a key for hiltViewModel to get unique instances
            val viewModel: StoryViewerViewModel = hiltViewModel(key = userId)

            LaunchedEffect(userId) {
                viewModel.loadStories(userId)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = (
                                (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                )

                        // 3D Cube Transition Logic
                        val rotation = 90f * pageOffset

                        // Perspective
                        cameraDistance = 8 * density

                        // Transform Origin and Rotation
                        if (pageOffset < 0) { // Incoming from right or outgoing to right
                            rotationY = rotation
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        } else if (pageOffset > 0) { // Incoming from left or outgoing to left
                            rotationY = rotation
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        } else {
                            rotationY = 0f
                        }

                        // Clip for clean edges
                        clip = true
                    }
            ) {
                StoryViewerScreen(
                    onFinished = {
                        if (pagerState.currentPage < userIds.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onClose()
                        }
                    },
                    onClose = onClose,
                    viewModel = viewModel,
                    isActive = pagerState.currentPage == pageIndex && !pagerState.isScrollInProgress
                )

                // Shading overlay to emphasize edges
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = (
                                    (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                    )
                            alpha = pageOffset.absoluteValue.coerceIn(0f, 0.7f)
                        }
                        .background(Color.Black)
                )
            }
        }
    }
}
