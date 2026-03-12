package com.synapse.social.studioasinc.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.synapse.social.studioasinc.domain.model.FeedItem
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.feature.home.home.FeedViewModel
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.PostActionsFactory
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.feature.shared.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper
import com.synapse.social.studioasinc.ui.components.ExpressivePullToRefreshIndicator
import com.synapse.social.studioasinc.feature.stories.tray.StoryTray
import com.synapse.social.studioasinc.feature.stories.tray.StoryTrayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = hiltViewModel(),
    storyTrayViewModel: StoryTrayViewModel = hiltViewModel(),
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onQuoteClick: (String) -> Unit,
    onMediaClick: (Int) -> Unit,
    onEditPost: (String) -> Unit,
    onStoryClick: (String) -> Unit = { _ -> },
    onAddStoryClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val storyTrayState by storyTrayViewModel.storyTrayState.collectAsStateWithLifecycle()
    val currentUser by storyTrayViewModel.currentUser.collectAsStateWithLifecycle()

    var isUserRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    val isRefreshing = isUserRefreshing

    // Handle block success/error messages
    LaunchedEffect(uiState.blockSuccess, uiState.blockError) {
        when {
            uiState.blockSuccess -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.block_success)
                )
                viewModel.clearBlockStatus()
            }
            uiState.blockError != null -> {
                snackbarHostState.showSnackbar(
                    message = uiState.blockError ?: context.getString(R.string.error_block_failed)
                )
                viewModel.clearBlockStatus()
            }
        }
    }

    val currentOnCommentClick by rememberUpdatedState(onCommentClick)
    val currentOnUserClick by rememberUpdatedState(onUserClick)
    val currentOnMediaClick by rememberUpdatedState(onMediaClick)
    val currentOnStoryClick by rememberUpdatedState(onStoryClick)



    val actions = remember(viewModel, currentOnCommentClick, currentOnUserClick, currentOnMediaClick) {
        PostActionsFactory.create(
            viewModel = viewModel,
            onComment = { post -> currentOnCommentClick(post.id) },
            onShare = viewModel::sharePost,
            onQuote = { post -> onQuoteClick(post.id) },
            onUserClick = { userId -> currentOnUserClick(userId) },
            onOptionClick = { post -> selectedPost = post },
            onMediaClick = { index -> currentOnMediaClick(index) }
        )
    }

    LaunchedEffect(isUserRefreshing) {
        if (isUserRefreshing) {
            // Give time for loaders to transition to Loading state
            kotlinx.coroutines.delay(500)
            
            // Wait for both loads to be done, with a safety timeout
            withTimeoutOrNull(15_000L) {
                snapshotFlow {
                    val isPostsDone = posts.loadState.refresh is LoadState.NotLoading || posts.loadState.refresh is LoadState.Error
                    val isStoriesDone = !storyTrayState.isLoading
                    isPostsDone && isStoriesDone
                }.first { it }
            }
            
            isUserRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isUserRefreshing = true
                    posts.refresh()
                    storyTrayViewModel.refresh()
                }
            },
            state = pullToRefreshState,
            indicator = {
                ExpressivePullToRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
        val isInitialLoading = posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0
        val isPreLoading = posts.loadState.refresh is LoadState.NotLoading && posts.itemCount == 0 && !posts.loadState.refresh.endOfPaginationReached && !posts.loadState.append.endOfPaginationReached
        val showLoading = (isInitialLoading || isPreLoading) && posts.itemCount == 0 && !isRefreshing
        val showError = posts.loadState.refresh is LoadState.Error && posts.itemCount == 0
        val showEmpty = posts.loadState.refresh is LoadState.NotLoading && 
                        posts.loadState.append.endOfPaginationReached && 
                        posts.itemCount == 0 && 
                        !isRefreshing

        if (showLoading) {
            FeedLoading()
        } else if (showError) {
            val e = posts.loadState.refresh as LoadState.Error
            FeedError(
                message = e.error.localizedMessage ?: stringResource(R.string.error_unknown_feed),
                onRetry = { posts.retry() }
            )
        } else if (showEmpty) {
            FeedEmpty()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = contentPadding
            ) {

                item(key = "story_tray") {


                    val onMyStoryClickMemoized = remember(storyTrayState.myStory) {
                        {
                            storyTrayState.myStory?.let { myStory ->
                                currentOnStoryClick(myStory.user.uid)
                            }
                            Unit
                        }
                    }
                    val onStoryClickMemoized = remember {
                        { storyWithUser: StoryWithUser ->
                            currentOnStoryClick(storyWithUser.user.uid)
                        }
                    }

                    StoryTray(
                        currentUser = currentUser,
                        myStory = storyTrayState.myStory,
                        friendStories = storyTrayState.friendStories,
                        onMyStoryClick = onMyStoryClickMemoized,
                        onAddStoryClick = onAddStoryClick,
                        onStoryClick = onStoryClickMemoized,
                        isLoading = storyTrayState.isLoading
                    )
                }

                // TODO: Implement "What's on your mind?" quick post area
                // - Add a Facebook-like composer card below StoryTray
                // - Show user avatar + "What's on your mind?" placeholder
                // - Tap to navigate to CreatePostScreen
                // - Include quick action buttons (Photo, Video, Feeling/Activity)

                items(
                    count = posts.itemCount,
                    key = posts.itemKey { it.id },
                    contentType = posts.itemContentType { it.itemType }
                ) { index ->
                    val feedItem = posts[index]
                    if (feedItem != null) {
                        when (feedItem) {
                            is FeedItem.PostItem -> {
                                SharedPostItem(
                                    post = feedItem.post,
                                    postViewStyle = uiState.postViewStyle,
                                    actions = actions
                                )
                            }
                            is FeedItem.CommentItem -> {
                                // Map FeedItem.CommentItem to PostCardState
                                val commentState = PostUiMapper.toPostCardState(feedItem)
                                
                                PostCard(
                                    state = commentState,
                                    postViewStyle = uiState.postViewStyle,
                                    onLikeClick = {
                                        viewModel.reactToComment(feedItem.id, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE)
                                    },
                                    onCommentClick = {
                                        // Still navigates to detail for now, but could open specific reply
                                        feedItem.parentPostId?.let { postId ->
                                            currentOnCommentClick(postId)
                                        }
                                    },
                                    onShareClick = {
                                        // Share comment link?
                                    },
                                    onRepostClick = {
                                        // Comments can be reshared too
                                        viewModel.resharePost(commentState.post)
                                    },
                                    onQuoteClick = {
                                        // Comments can be quoted too
                                        viewModel.quotePost(commentState.post, "") 
                                    },
                                    onBookmarkClick = {
                                        viewModel.bookmarkPost(commentState.post)
                                    },
                                    onUserClick = {
                                        currentOnUserClick(feedItem.userId)
                                    },
                                    onPostClick = {
                                        feedItem.parentPostId?.let { postId ->
                                            currentOnCommentClick(postId)
                                        }
                                    },
                                    onMediaClick = { index ->
                                        // Comments might have media
                                        currentOnMediaClick(index)
                                    },
                                    onOptionsClick = {
                                        // Map minimal post for options
                                        selectedPost = commentState.post
                                    },
                                    onPollVote = { _ -> },
                                    onReactionSelected = { reaction ->
                                        viewModel.reactToComment(feedItem.id, reaction)
                                    },
                                    onParentAuthorClick = {
                                        // Navigate to parent author
                                    }
                                )
                            }
                        }
                    }
                }

                if (posts.loadState.append is LoadState.Loading) {
                    item { PostShimmer() }
                }

                if (posts.loadState.append is LoadState.Error) {
                    item {
                        val e = posts.loadState.append as LoadState.Error
                        FeedError(
                            message = stringResource(R.string.error_loading_more_posts),
                            onRetry = { posts.retry() },
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                }
            }
        }
    }
    
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }

    selectedPost?.let { post ->
        PostOptionsBottomSheet(
            post = post,
            isOwner = viewModel.isPostOwner(post),
            commentsDisabled = viewModel.areCommentsDisabled(post),
            onDismiss = { selectedPost = null },
            onEdit = { onEditPost(post.id) },
            onDelete = { viewModel.deletePost(post) },
            onShare = { viewModel.sharePost(post) },
            onCopyLink = { viewModel.copyPostLink(post) },
            onBookmark = { viewModel.bookmarkPost(post) },
            onToggleComments = { viewModel.toggleComments(post) },
            onReport = { viewModel.reportPost(post) },
            onBlock = { viewModel.blockUser(post.authorUid) },
            onRevokeVote = { viewModel.revokeVote(post) }
        )
    }
}
