package com.synapse.social.studioasinc.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.search.search.components.AccountCard
import com.synapse.social.studioasinc.feature.search.search.components.HashtagCard
import com.synapse.social.studioasinc.feature.search.search.components.NewsCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.feature.shared.components.post.PostSummarySheet
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle
import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPost: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val postLinkLabel = stringResource(R.string.clip_label_post_link)
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var showSummarySheet by remember { mutableStateOf(false) }


    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { SearchTab.entries.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (uiState.selectedTab.ordinal != page) {
                viewModel.onTabSelected(SearchTab.entries[page])
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }
    
    // Handle block success/error messages
    LaunchedEffect(uiState.blockSuccess, uiState.blockError) {
        when {
            uiState.blockSuccess -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(com.synapse.social.studioasinc.R.string.block_success)
                )
                viewModel.clearBlockStatus()
            }
            uiState.blockError != null -> {
                snackbarHostState.showSnackbar(
                    message = uiState.blockError ?: context.getString(com.synapse.social.studioasinc.R.string.error_block_failed)
                )
                viewModel.clearBlockStatus()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = uiState.query,
                        onQueryChange = viewModel::onQueryChange,
                        onSearch = viewModel::onSearch,
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.search_synapse_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.query.isNotEmpty()) {
                                    IconButton(onClick = viewModel::clearSearch) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                                    }
                                } else {
                                    IconButton(onClick = { Toast.makeText(context, context.getString(R.string.voice_search), Toast.LENGTH_SHORT).show() }) {
                                        Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.voice_search))
                                    }
                                }
                            }
                        }
                    )
                },
                expanded = false,
                onExpandedChange = {},
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    dividerColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            ) {
                // Search History
                val history = uiState.searchHistory
                if (history.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.recent_searches),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.Medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        history.forEach { query ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onSearch(query) },
                                label = { Text(query) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = Spacing.Small,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
            ) {
                SearchTab.entries.forEach { tab ->
                    Tab(
                        selected = pagerState.currentPage == tab.ordinal,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (pagerState.currentPage == tab.ordinal) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }


            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val tab = SearchTab.entries[page]

                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoading) {
                        SearchLoadingShimmer()
                    } else {
                        when (tab) {
                            SearchTab.FOR_YOU -> {
                                if (uiState.posts.isEmpty() && uiState.accounts.isEmpty() && uiState.hashtags.isEmpty()) {
                                    EmptyState(stringResource(R.string.no_content_found))
                                } else {
                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(Spacing.Medium),
                                        verticalItemSpacing = Spacing.Medium,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                                    ) {
                                        items(uiState.hashtags) { hashtag ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(Spacing.Medium),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                HashtagCard(
                                                    hashtag = hashtag,
                                                    onClick = { viewModel.onSearch(hashtag.tag) }
                                                )
                                            }
                                        }
                                        items(uiState.accounts) { account ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(Spacing.Medium),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                AccountCard(
                                                    account = account,
                                                    onClick = { onNavigateToProfile(account.id) },
                                                    onFollowClick = { viewModel.toggleFollow(account.id) }
                                                )
                                            }
                                        }
                                        items(uiState.posts) { post ->
                                            val actions = remember(viewModel) {
                                                PostActions(
                                                    onLike = viewModel::likePost,
                                                    onComment = { p -> onNavigateToPost(p.id) },
                                                    onShare = viewModel::sharePost,
                                                    onRepost = { },
                                                    onQuote = { },
                                                    onBookmark = viewModel::bookmarkPost,
                                                    onOptionClick = { p -> selectedPost = p },
                                                    onPollVote = viewModel::votePoll,
                                                    onUserClick = { userId -> onNavigateToProfile(userId) },
                                                    onMediaClick = { _ -> onNavigateToPost(post.id) },
                                                    onReactionSelected = { p, r -> viewModel.reactToPost(p, r) }
                                                )
                                            }
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(Spacing.Medium),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                SharedPostItem(
                                                    post = post,
                                                    actions = actions
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = Sizes.WidthLarge)
                                ) {
                                    when (tab) {
                                        SearchTab.FOR_YOU -> {} // Handled above
                                SearchTab.PEOPLE -> {
                                    if (uiState.accounts.isEmpty()) {
                                        item { EmptyState(stringResource(R.string.no_accounts_found)) }
                                    } else {
                                        itemsIndexed(uiState.accounts, key = { index, it -> "${it.id}_${index}" }) { index, account ->
                                            AccountCard(
                                                account = account,
                                                onClick = { onNavigateToProfile(account.id) },
                                                onFollowClick = { viewModel.toggleFollow(account.id) }
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = Sizes.BorderHairline
                                            )
                                        }
                                    }
                                }
                                SearchTab.TRENDING, SearchTab.LISTS -> {
                                    if (uiState.hashtags.isEmpty()) {
                                        item { EmptyState(stringResource(R.string.no_hashtags_found)) }
                                    } else {
                                        itemsIndexed(uiState.hashtags, key = { index, it -> "${it.id}_${index}" }) { index, hashtag ->
                                            HashtagCard(
                                                hashtag = hashtag,
                                                onClick = { viewModel.onSearch(hashtag.tag) }
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = Sizes.BorderHairline
                                            )
                                        }
                                    }
                                }
                                SearchTab.NEWS, SearchTab.SPORTS, SearchTab.ENTERTAINMENT -> {
                                    if (uiState.news.isEmpty()) {
                                        item { EmptyState(stringResource(R.string.no_news_found)) }
                                    } else {
                                        itemsIndexed(uiState.news, key = { index, it -> "${it.id}_${index}" }) { index, news ->
                                            NewsCard(
                                                news = news,
                                                onClick = {
                                                    news.url?.let { url ->
                                                        IntentUtils.openUrl(context, url)
                                                    }
                                                }
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = Sizes.BorderHairline
                                            )
                                        }
                                    }
                                }
                                SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> {
                                    if (uiState.posts.isEmpty()) {
                                        item { EmptyState(stringResource(R.string.empty_posts_title)) }
                                    } else {
                                        itemsIndexed(uiState.posts, key = { index, it -> "${it.id}_${index}" }) { index, post ->
                                            val actions = remember(viewModel) {
                                                PostActions(
                                                    onLike = viewModel::likePost,
                                                    onComment = { p -> onNavigateToPost(p.id) },
                                                    onShare = viewModel::sharePost,
                                                    onRepost = { },
                                                    onQuote = { },
                                                    onBookmark = viewModel::bookmarkPost,
                                                    onOptionClick = { p -> selectedPost = p },
                                                    onPollVote = viewModel::votePoll,
                                                    onUserClick = { userId -> onNavigateToProfile(userId) },
                                                    onMediaClick = { _ -> onNavigateToPost(post.id) },
                                                    onReactionSelected = { p, r -> viewModel.reactToPost(p, r) }
                                                )
                                            }

                                            SharedPostItem(
                                                post = post,
                                                actions = actions
                                            )
                                        }
                                    }
                                }
                            } // End inner when
                        } // End LazyColumn
                    } // End else branch
                } // End outer when
            } // End Box else
        } // End HorizontalPager Box
    } // End HorizontalPager content
} // End Column scope
} // End Scaffold innerPadding scope

    selectedPost?.let { post ->
        PostOptionsBottomSheet(
            post = post,
            isOwner = viewModel.isPostOwner(post),
            commentsDisabled = viewModel.areCommentsDisabled(post),
            onDismiss = { selectedPost = null },
            onEdit = {  },
            onDelete = {
                viewModel.deletePost(post)
                selectedPost = null
            },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_synapse_text, post.id))
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.title_share_post)))
                selectedPost = null
            },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(postLinkLabel, "synapse://post/${post.id}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                selectedPost = null
            },
            onBookmark = {
                viewModel.bookmarkPost(post)
                selectedPost = null
            },
            onToggleComments = {
                viewModel.toggleComments(post)
                selectedPost = null
            },
            onReport = {
                viewModel.reportPost(post)
                selectedPost = null
            },
            onBlock = {
                viewModel.blockUser(post.authorUid)
                selectedPost = null
            },
            onRevokeVote = {
                viewModel.revokeVote(post)
                selectedPost = null
            },
            onSummarize = {
                showSummarySheet = true
                viewModel.summarizePost(post)
            }
        )
    }

    if (showSummarySheet) {
        PostSummarySheet(
            isSummarizing = uiState.isSummarizing,
            summary = uiState.postSummary,
            error = uiState.summaryError,
            onDismiss = {
                showSummarySheet = false
                viewModel.clearPostSummary()
            }
        )
    }
}

@Composable
fun SearchLoadingShimmer() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium)
    ) {
        items(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.SmallMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerCircle(size = Spacing.Huge)
                Spacer(modifier = Modifier.width(Spacing.Medium))
                Column {
                    ShimmerBox(modifier = Modifier.width(Sizes.ShimmerWidthLarge).height(Spacing.Medium))
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    ShimmerBox(modifier = Modifier.width(80.dp).height(Spacing.SmallMedium))
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = Sizes.BorderHairline
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.ExtraLarge),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


