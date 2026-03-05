package com.synapse.social.studioasinc.feature.shared.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.shared.reels.components.MoreActionsBottomSheet
import com.synapse.social.studioasinc.feature.shared.reels.components.ShareBottomSheet
import com.synapse.social.studioasinc.ui.components.ShimmerBox

@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel = hiltViewModel(),
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reels = uiState.reels

    var showMoreActionsForReelId by remember { mutableStateOf<String?>(null) }
    var showShareSheetForReelUrl by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState(pageCount = {
        if (uiState.isEndReached) reels.size else reels.size + 1
    })


    LaunchedEffect(pagerState.currentPage, reels) {
        val index = pagerState.currentPage
        if (index < reels.size) {
            val urlsToPreload = reels.drop(index + 1).take(3).map { it.videoUrl }
            if (urlsToPreload.isNotEmpty()) {
                viewModel.preloadReels(urlsToPreload)
            }

            // Proactively release players for reels just out of the window to save memory
            // without looping over the entire list.
            val outOfBoundsBefore = index - 3
            if (outOfBoundsBefore >= 0 && outOfBoundsBefore < reels.size) {
                viewModel.releasePlayer(reels[outOfBoundsBefore].videoUrl)
            }
            val outOfBoundsAfter = index + 3
            if (outOfBoundsAfter < reels.size) {
                viewModel.releasePlayer(reels[outOfBoundsAfter].videoUrl)
            }
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseAllPlayers()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page >= reels.size - 2 && !uiState.isEndReached && !uiState.isLoadMoreLoading) {
                viewModel.loadMoreReels()
            }
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            viewModel.releaseAllPlayers()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && reels.isEmpty()) {
            ReelShimmerItem()
        } else if (uiState.error != null && reels.isEmpty()) {
            ErrorFeed(
                message = uiState.error ?: "Failed to load reels",
                onRetry = { viewModel.loadReels() }
            )
        } else if (!uiState.isLoading && reels.isEmpty()) {
            EmptyFeed(onRetry = { viewModel.loadReels() })
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) { page ->
                if (page < reels.size) {
                    val reel = reels[page]
                    ReelItem(
                        reel = reel,
                        isActive = page == pagerState.currentPage,
                        onLikeClick = { viewModel.likeReel(reel.id) },
                        onOpposeClick = { viewModel.opposeReel(reel.id) },
                        onCommentClick = { onCommentClick(reel.id) },
                        onShareClick = { showShareSheetForReelUrl = reel.videoUrl },
                        onMoreClick = { showMoreActionsForReelId = reel.id },
                        onUserClick = { onUserClick(reel.creatorId) },
                        onBackClick = onBackClick
                    )
                } else if (!uiState.isEndReached) {
                    ReelShimmerItem()
                }
            }
        }

    }

    showMoreActionsForReelId?.let { reelId ->
        val reel = reels.find { it.id == reelId }
        MoreActionsBottomSheet(
            onDismiss = { showMoreActionsForReelId = null },
            onReport = { viewModel.reportReel(reelId, "Inappropriate content") },
            onBlock = { reel?.let { viewModel.blockCreator(it.creatorId) } },
            onDownload = { reel?.let { viewModel.downloadReel(it.videoUrl) } }
        )
    }

    showShareSheetForReelUrl?.let { videoUrl ->
        ShareBottomSheet(
            videoUrl = videoUrl,
            onDismiss = { showShareSheetForReelUrl = null },
            onShareExternal = { viewModel.shareReel(videoUrl) }
        )
    }
}

@Composable
fun ErrorFeed(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun EmptyFeed(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No reels found.", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Refresh")
        }
    }
}

@Composable
fun ReelShimmerItem() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(32.dp), shape = androidx.compose.foundation.shape.CircleShape)
                Spacer(modifier = Modifier.size(8.dp))
                ShimmerBox(modifier = Modifier.width(100.dp).height(20.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
            Spacer(modifier = Modifier.size(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(16.dp))
        }


        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(4) {
                ShimmerBox(modifier = Modifier.size(32.dp), shape = androidx.compose.foundation.shape.CircleShape)
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}
