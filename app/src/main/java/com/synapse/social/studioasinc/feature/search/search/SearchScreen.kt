package com.synapse.social.studioasinc.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle
import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
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
    var selectedPost by remember { mutableStateOf<Post?>(null) }


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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {

            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::onSearch,
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search Synapse") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                     if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = { Toast.makeText(context, "QR Scanner feature coming soon", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                        }
                    }
                },
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    dividerColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {}
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp,
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            when (tab) {
                                SearchTab.FOR_YOU, SearchTab.PEOPLE -> {
                                    if (uiState.accounts.isEmpty()) {
                                        item { EmptyState("No accounts found") }
                                    } else {
                                        items(uiState.accounts, key = { it.id }) { account ->
                                            AccountCard(
                                                account = account,
                                                onClick = { onNavigateToProfile(account.id) },
                                                onFollowClick = { viewModel.toggleFollow(account.id) }
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                                SearchTab.TRENDING, SearchTab.LISTS -> {
                                    if (uiState.hashtags.isEmpty()) {
                                        item { EmptyState("No hashtags found") }
                                    } else {
                                        items(uiState.hashtags, key = { it.id }) { hashtag ->
                                            HashtagCard(
                                                hashtag = hashtag,
                                                onClick = { viewModel.onSearch(hashtag.tag) }
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                                SearchTab.NEWS, SearchTab.SPORTS, SearchTab.ENTERTAINMENT -> {
                                    if (uiState.news.isEmpty()) {
                                        item { EmptyState("No news found") }
                                    } else {
                                        items(uiState.news, key = { it.id }) { news ->
                                            NewsCard(
                                                news = news,
                                                onClick = {
                                                    news.url?.let { url ->
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                                SearchTab.TOP, SearchTab.LATEST, SearchTab.MEDIA -> {
                                    if (uiState.posts.isEmpty()) {
                                        item { EmptyState("No posts found") }
                                    } else {
                                        items(uiState.posts, key = { it.id }) { searchPost ->
                                            val post = remember(searchPost) { searchPost.toPost() }

                                            val actions = remember(viewModel) {
                                                PostActions(
                                                    onLike = viewModel::likePost,
                                                    onComment = { post -> onNavigateToPost(post.id) },
                                                    onShare = viewModel::sharePost,
                                                    onRepost = { },
                                                    onBookmark = viewModel::bookmarkPost,
                                                    onOptionClick = { post -> selectedPost = post },
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
                            }
                        }
                    }
                }
            }
        }
    }

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
                    putExtra(Intent.EXTRA_TEXT, "Check out this post on Synapse: synapse://post/${post.id}")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                selectedPost = null
            },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Post Link", "synapse://post/${post.id}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
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
                    ShimmerBox(modifier = Modifier.width(120.dp).height(Spacing.Medium))
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    ShimmerBox(modifier = Modifier.width(80.dp).height(Spacing.SmallMedium))
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


private fun SearchPost.toPost(): Post {
    return Post(
        id = this.id,
        authorUid = this.authorId,
        postText = this.content,
        publishDate = this.createdAt,
        timestamp = 0L,
        likesCount = this.likesCount,
        commentsCount = this.commentsCount,
        resharesCount = this.boostCount,
        username = this.authorHandle,
        avatarUrl = this.authorAvatar,
        mediaItems = null
    )
}
