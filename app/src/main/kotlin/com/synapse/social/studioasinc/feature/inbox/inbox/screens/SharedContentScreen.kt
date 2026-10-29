package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.SharedContentViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.*
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedContentScreen(
    chatId: String,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    viewModel: SharedContentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatId) {
        viewModel.loadContent(chatId, initialTab)
    }

    val uiState by viewModel.uiState.collectAsState()

    // Handle item clicks
    LaunchedEffect(uiState.selectedLinkItem) {
        uiState.selectedLinkItem?.let { item ->
            openLink(context, item.url)
            viewModel.clearSelection()
        }
    }

    LaunchedEffect(uiState.selectedFileItem) {
        uiState.selectedFileItem?.let { item ->
            openFile(context, item.url, item.fileName)
            viewModel.clearSelection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shared_content_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row with counts
            val tabs = listOf(
                stringResource(R.string.tab_media) to uiState.mediaCount,
                stringResource(R.string.tab_links) to uiState.linkCount,
                stringResource(R.string.tab_files) to uiState.fileCount
            )

            PrimaryTabRow(selectedTabIndex = uiState.currentTab) {
                tabs.forEachIndexed { index, (title, count) ->
                    Tab(
                        selected = uiState.currentTab == index,
                        onClick = { viewModel.switchTab(index) },
                        text = {
                            Text(
                                text = if (count > 0) "$title ($count)" else title,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading && !uiState.isRefreshing -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.errorMessageRes != null && uiState.mediaItems.isEmpty() -> {
                        val errorMessageRes = uiState.errorMessageRes
                        if (errorMessageRes != null) {
                            SharedContentErrorState(
                                message = stringResource(errorMessageRes),
                                onRetry = { viewModel.refreshContent() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    else -> {
                        when (uiState.currentTab) {
                            0 -> MediaTabContent(
                                items = uiState.mediaItems,
                                hasMore = uiState.hasMoreMedia,
                                isLoadingMore = uiState.isLoadingMore,
                                onItemClick = { viewModel.onMediaItemClicked(it) },
                                onLoadMore = { viewModel.loadMoreContent() },
                                modifier = Modifier.fillMaxSize()
                            )

                            1 -> LinksTabContent(
                                items = uiState.linkItems,
                                hasMore = uiState.hasMoreLinks,
                                isLoadingMore = uiState.isLoadingMore,
                                onItemClick = { viewModel.onLinkItemClicked(it) },
                                onLoadMore = { viewModel.loadMoreContent() },
                                modifier = Modifier.fillMaxSize()
                            )

                            2 -> FilesTabContent(
                                items = uiState.fileItems,
                                hasMore = uiState.hasMoreFiles,
                                isLoadingMore = uiState.isLoadingMore,
                                onItemClick = { viewModel.onFileItemClicked(it) },
                                onLoadMore = { viewModel.loadMoreContent() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Pull-to-refresh indicator
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(Spacing.space16)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaTabContent(
    items: List<com.synapse.social.studioasinc.shared.domain.model.chat.MediaItem>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onItemClick: (com.synapse.social.studioasinc.shared.domain.model.chat.MediaItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, hasMore, isLoadingMore) {
        if (hasMore && !isLoadingMore) {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null && lastVisibleItem.index >= items.size - 6) {
                onLoadMore()
            }
        }
    }

    if (items.isEmpty()) {
        SharedContentEmptyState(
            message = stringResource(R.string.empty_shared_content),
            modifier = modifier
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(Spacing.space8),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8),
            modifier = modifier
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                MediaGridItem(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.space16),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(Sizes.IconMedium))
                    }
                }
            }
        }
    }
}

@Composable
private fun LinksTabContent(
    items: List<com.synapse.social.studioasinc.shared.domain.model.chat.LinkItem>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onItemClick: (com.synapse.social.studioasinc.shared.domain.model.chat.LinkItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, hasMore, isLoadingMore) {
        if (hasMore && !isLoadingMore) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null && lastVisibleItem.index >= items.size - 3) {
                onLoadMore()
            }
        }
    }

    if (items.isEmpty()) {
        SharedContentEmptyState(
            message = stringResource(R.string.empty_shared_content),
            modifier = modifier
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8),
            modifier = modifier
        ) {
            items(
                count = items.size,
                key = { index -> items[index].id }
            ) { index ->
                val item = items[index]
                LinkPreviewCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.space16),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(Sizes.IconMedium))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTabContent(
    items: List<com.synapse.social.studioasinc.shared.domain.model.chat.FileItem>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onItemClick: (com.synapse.social.studioasinc.shared.domain.model.chat.FileItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, hasMore, isLoadingMore) {
        if (hasMore && !isLoadingMore) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem != null && lastVisibleItem.index >= items.size - 3) {
                onLoadMore()
            }
        }
    }

    if (items.isEmpty()) {
        SharedContentEmptyState(
            message = stringResource(R.string.empty_shared_content),
            modifier = modifier
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8),
            modifier = modifier
        ) {
            items(
                count = items.size,
                key = { index -> items[index].id }
            ) { index ->
                val item = items[index]
                FileListItem(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.space16),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(Sizes.IconMedium))
                    }
                }
            }
        }
    }
}

private fun openLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

private fun openFile(context: Context, url: String, fileName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}
