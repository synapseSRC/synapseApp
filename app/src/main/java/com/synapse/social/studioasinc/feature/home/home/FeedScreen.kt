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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
    onMediaClick: (Int) -> Unit,
    onEditPost: (String) -> Unit,
    onStoryClick: (String) -> Unit = { _ -> },
    onAddStoryClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    var selectedPost by remember { mutableStateOf<Post?>(null) }


    val storyTrayState by storyTrayViewModel.storyTrayState.collectAsStateWithLifecycle()
    val currentUser by storyTrayViewModel.currentUser.collectAsStateWithLifecycle()

    var isUserRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    val isRefreshing = isUserRefreshing || posts.loadState.refresh is LoadState.Loading



    val currentOnCommentClick by rememberUpdatedState(onCommentClick)
    val currentOnUserClick by rememberUpdatedState(onUserClick)
    val currentOnMediaClick by rememberUpdatedState(onMediaClick)
    val currentOnStoryClick by rememberUpdatedState(onStoryClick)



    val actions = remember(viewModel, currentOnCommentClick, currentOnUserClick, currentOnMediaClick) {
        PostActionsFactory.create(
            viewModel = viewModel,
            onComment = { post -> currentOnCommentClick(post.id) },
            onShare = viewModel::sharePost,
            onUserClick = { userId -> currentOnUserClick(userId) },
            onOptionClick = { post -> selectedPost = post },
            onMediaClick = { index -> currentOnMediaClick(index) }
        )
    }

    LaunchedEffect(posts.loadState.refresh) {
        if (posts.loadState.refresh is LoadState.NotLoading) {
            isUserRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isUserRefreshing = true
            posts.refresh()
            viewModel.refresh()
            storyTrayViewModel.refresh()
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
        if (posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0) {
            FeedLoading()
        } else if (posts.loadState.refresh is LoadState.Error) {
            val e = posts.loadState.refresh as LoadState.Error
            FeedError(
                message = e.error.localizedMessage ?: stringResource(R.string.error_unknown_feed),
                onRetry = { posts.retry() }
            )
        } else if (posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading) {
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
                                        // Optional: Handle comment liking in future
                                    },
                                    onCommentClick = {
                                        // Navigate to parent post to show comment
                                        feedItem.parentPostId?.let { postId ->
                                            currentOnCommentClick(postId)
                                        }
                                    },
                                    onShareClick = {
                                        // Optional: Handle comment sharing in future
                                    },
                                    onRepostClick = {
                                        // Not applicable for comments
                                    },
                                    onBookmarkClick = {
                                        // Not applicable for comments
                                    },
                                    onUserClick = {
                                        currentOnUserClick(feedItem.userId)
                                    },
                                    onPostClick = {
                                        // Navigate to parent post to show comment
                                        feedItem.parentPostId?.let { postId ->
                                            currentOnCommentClick(postId)
                                        }
                                    },
                                    onMediaClick = { index ->
                                        // Not applicable for comments
                                    },
                                    onOptionsClick = {
                                        // Optional: Handle comment options in future
                                    },
                                    onPollVote = { _ ->
                                        // Not applicable for comments
                                    },
                                    onReactionSelected = null,
                                    onParentAuthorClick = {
                                        // Navigate to parent author if available
                                        feedItem.parentAuthorUsername?.let { username ->
                                            // Note: We need userId, not username for navigation
                                            // This is a limitation - we may need to enhance FeedItem.CommentItem
                                            // For now, clicking parent author will do nothing
                                        }
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
