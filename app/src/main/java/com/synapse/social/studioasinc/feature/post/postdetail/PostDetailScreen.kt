package com.synapse.social.studioasinc.feature.post.postdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.synapse.social.studioasinc.domain.model.CommentAction
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.feature.post.postdetail.components.*
import com.synapse.social.studioasinc.feature.shared.components.MediaViewer
import com.synapse.social.studioasinc.feature.shared.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.feature.shared.components.ReportPostDialog
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.ReactionPicker
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.paging.compose.LazyPagingItems


@Composable
fun PostDetailScreen(
    postId: String,
    rootCommentId: String? = null,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToEditPost: (String) -> Unit,
    onNavigateToCommentDetail: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagingItems = viewModel.commentsPagingFlow.collectAsLazyPagingItems()

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableIntStateOf(0) }
    var showPostOptions by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var showCommentOptions by remember { mutableStateOf<CommentWithUser?>(null) }
    var showReactionPickerForComment by remember { mutableStateOf<CommentWithUser?>(null) }

    val currentUserId = uiState.currentUserId

    LaunchedEffect(postId, rootCommentId) {
        viewModel.loadPost(postId, rootCommentId)
    }

    LaunchedEffect(uiState.refreshTrigger) {
        if (uiState.refreshTrigger > 0) {
            pagingItems.refresh()
        }
    }
    
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

    if (showMediaViewer && uiState.post != null) {
        val mediaUrls = uiState.post!!.post.mediaItems?.map { it.url }
            ?: listOfNotNull(uiState.post!!.post.postImage)

        MediaViewer(
            mediaUrls = mediaUrls,
            initialPage = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }

    if (showPostOptions && uiState.post != null) {
        val postDetail = uiState.post!!
        PostOptionsBottomSheet(
            post = postDetail.post,
            isOwner = currentUserId == postDetail.post.authorUid,
            commentsDisabled = postDetail.post.postDisableComments == "true",
            onDismiss = { showPostOptions = false },
            onEdit = { onNavigateToEditPost(postId) },
            onDelete = {
                viewModel.deletePost(postId)
                onNavigateBack()
            },
            onShare = { sharePost(context, postId) },
            onCopyLink = { viewModel.copyLink(postId, context) },
            onBookmark = { viewModel.toggleBookmark() },
            onToggleComments = { viewModel.toggleComments() },
            onReport = { showReportDialog = true },
            onBlock = { viewModel.blockUser(postDetail.post.authorUid) },
            onRevokeVote = { viewModel.revokeVote() }
        )
    }

    if (showReportDialog) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                viewModel.reportPost(reason)
                showReportDialog = false
                Toast.makeText(context, context.getString(R.string.toast_report_submitted), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showReactionPickerForComment != null) {
        ReactionPicker(
            onReactionSelected = { reaction ->
                viewModel.toggleCommentReaction(showReactionPickerForComment!!.id, reaction)
                showReactionPickerForComment = null
            },
            onDismiss = { showReactionPickerForComment = null }
        )
    }

    if (showCommentOptions != null) {
        val comment = showCommentOptions!!
        CommentOptionsBottomSheet(
            comment = comment,
            isOwnComment = currentUserId == comment.userId,
            isPostAuthor = currentUserId == uiState.post?.post?.authorUid,
            onDismiss = { showCommentOptions = null },
            onAction = { action ->
                handleCommentAction(
                    action = action,
                    comment = comment,
                    context = context,
                    viewModel = viewModel,
                    focusRequester = focusRequester,
                    keyboardController = keyboardController,
                    scope = scope
                )
                showCommentOptions = null
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { PostDetailTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            PostDetailBottomBar(
                uiState = uiState,
                focusRequester = focusRequester,
                onCancelReply = { viewModel.setReplyTo(null) },
                onCancelEdit = { viewModel.setEditingComment(null) },
                onSendComment = { content, mediaUri ->
                    if (uiState.editingComment != null) {
                        viewModel.editComment(uiState.editingComment!!.id, content)
                    } else {
                        viewModel.addComment(content, mediaUri)
                    }
                }
            )
        }
    ) { paddingValues ->
        PostDetailContent(
            uiState = uiState,
            paddingValues = paddingValues,
            pagingItems = pagingItems,
            onReplyClick = { comment ->
                viewModel.setReplyTo(comment)
                scope.launch {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
            onLikeClick = { commentId -> viewModel.toggleCommentReaction(commentId, ReactionType.LIKE) },
            onShowReactions = { showReactionPickerForComment = it },
            onShowOptions = { showCommentOptions = it },
            onUserClick = onNavigateToProfile,
            onViewReplies = { commentId -> viewModel.loadReplies(commentId) },
            onCommentClick = { commentId -> onNavigateToCommentDetail(postId, commentId) },
            onPostLike = { _ -> viewModel.toggleReaction(ReactionType.LIKE) },
            onPostComment = { _ ->
                scope.launch {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
            onPostShare = { _ -> sharePost(context, postId) },
            onPostRepost = { _ -> viewModel.createReshare(null) },
            onPostBookmark = { _ -> viewModel.toggleBookmark() },
            onPostOptionClick = { _ -> showPostOptions = true },
            onPostPollVote = { _, index -> viewModel.votePoll(index) },
            onPostMediaClick = { index ->
                selectedMediaIndex = index
                showMediaViewer = true
            },
            onPostReactionSelected = { _, reaction -> viewModel.toggleReaction(reaction) }
        )
    }
}

@Composable
private fun PostDetailContent(
    uiState: PostDetailUiState,
    paddingValues: PaddingValues,
    pagingItems: androidx.paging.compose.LazyPagingItems<CommentWithUser>,
    onReplyClick: (CommentWithUser) -> Unit,
    onLikeClick: (String) -> Unit,
    onShowReactions: (CommentWithUser) -> Unit,
    onShowOptions: (CommentWithUser) -> Unit,
    onUserClick: (String) -> Unit,
    onViewReplies: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onPostLike: (com.synapse.social.studioasinc.domain.model.Post) -> Unit,
    onPostComment: (com.synapse.social.studioasinc.domain.model.Post) -> Unit,
    onPostShare: (com.synapse.social.studioasinc.domain.model.Post) -> Unit,
    onPostRepost: (com.synapse.social.studioasinc.domain.model.Post) -> Unit,
    onPostBookmark: (com.synapse.social.studioasinc.domain.model.Post) -> Unit,
    onPostOptionClick: (com.synapse.social.studioasinc.domain.model.Post) -> Unit,
    onPostPollVote: (com.synapse.social.studioasinc.domain.model.Post, Int) -> Unit,
    onPostMediaClick: (Int) -> Unit,
    onPostReactionSelected: (com.synapse.social.studioasinc.domain.model.Post, ReactionType) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (uiState.isLoading && uiState.post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ExpressiveLoadingIndicator()
            }
        } else if (uiState.error != null && uiState.post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error_prefix, uiState.error ?: ""))
            }
        } else {
            val postDetail = uiState.post
            if (postDetail != null) {
                CommentsList(
                    comments = pagingItems,
                    repliesState = uiState.replies,
                    replyLoadingState = uiState.replyLoading,
                    commentActionsLoading = uiState.commentActionsLoading,
                    onReplyClick = onReplyClick,
                    onLikeClick = onLikeClick,
                    onShowReactions = onShowReactions,
                    onShowOptions = onShowOptions,
                    onUserClick = onUserClick,
                    onViewReplies = onViewReplies,
                    onCommentClick = onCommentClick,
                    modifier = Modifier.fillMaxSize(),
                    headerContent = {
                        if (uiState.rootComment != null) {
                            val rootComment = uiState.rootComment
                            
                            // Render ancestors
                            uiState.ancestorComments.forEach { ancestor ->
                                val ancestorState = PostUiMapper.toPostCardState(
                                    comment = ancestor,
                                    parentAuthorUsername = null,
                                    depth = 0,
                                    showThreadLine = true,
                                    isThreadChild = false,
                                    isLastReply = false
                                )
                                PostCard(
                                    state = ancestorState,
                                    onLikeClick = { onLikeClick(ancestor.id) },
                                    onCommentClick = { onReplyClick(ancestor) },
                                    onShareClick = { /* Implement share */ },
                                    onRepostClick = { },
                                    onBookmarkClick = { },
                                    onUserClick = { ancestor.userId?.let { onUserClick(it) } },
                                    onPostClick = { onCommentClick(ancestor.id) },
                                    onMediaClick = { },
                                    onOptionsClick = { onShowOptions(ancestor) },
                                    onPollVote = { },
                                    onReactionSelected = { onShowReactions(ancestor) },
                                    onQuoteClick = { },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            val postCardState = PostUiMapper.toPostCardState(
                                comment = rootComment,
                                parentAuthorUsername = null,
                                depth = 0,
                                showThreadLine = false,
                                isThreadChild = uiState.ancestorComments.isNotEmpty(),
                                isLastReply = false
                            ).copy(isExpanded = true)

                            PostCard(
                                state = postCardState,
                                onLikeClick = { onLikeClick(rootComment.id) },
                                onCommentClick = { onReplyClick(rootComment) },
                                onShareClick = { /* Implement share */ },
                                onRepostClick = { },
                                onBookmarkClick = { },
                                onUserClick = { rootComment.userId?.let { onUserClick(it) } },
                                onPostClick = { },
                                onMediaClick = { },
                                onOptionsClick = { onShowOptions(rootComment) },
                                onPollVote = { },
                                onReactionSelected = { onShowReactions(rootComment) },
                                onQuoteClick = { },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            val mergedPost = remember(postDetail) {
                                postDetail.post.copy(
                                    userReaction = postDetail.userReaction,
                                    reactions = postDetail.reactionSummary,
                                    likesCount = postDetail.reactionSummary.values.sum(),
                                    commentsCount = postDetail.post.commentsCount,
                                    username = postDetail.author.username,
                                    avatarUrl = postDetail.author.avatar,
                                    isVerified = postDetail.author.verify
                                )
                            }

                            val actions = remember(onPostLike, onPostComment, onPostShare, onPostRepost, onPostBookmark, onPostOptionClick, onPostPollVote, onUserClick, onPostMediaClick, onPostReactionSelected) {
                                PostActions(
                                    onLike = onPostLike,
                                    onComment = onPostComment,
                                    onShare = onPostShare,
                                    onRepost = onPostRepost,
                                    onBookmark = onPostBookmark,
                                    onOptionClick = onPostOptionClick,
                                    onPollVote = onPostPollVote,
                                    onUserClick = onUserClick,
                                    onMediaClick = onPostMediaClick,
                                    onReactionSelected = onPostReactionSelected,
                                    onQuote = onPostRepost
                                )
                            }

                            SharedPostItem(
                                post = mergedPost,
                                actions = actions,
                                isExpanded = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.title_post_detail)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
        },
        modifier = Modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun PostDetailBottomBar(
    uiState: PostDetailUiState,
    focusRequester: FocusRequester,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onSendComment: (String, android.net.Uri?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.replyToComment != null) {
            ReplyIndicator(
                replyTo = uiState.replyToComment!!,
                onCancelReply = onCancelReply
            )
        }
        if (uiState.editingComment != null) {
            EditIndicator(
                comment = uiState.editingComment!!,
                onCancel = onCancelEdit
            )
        }
        CommentInput(
            onSend = onSendComment,
            initialValue = uiState.editingComment?.content ?: "",
            userAvatarUrl = uiState.currentUserAvatarUrl,
            replyingToUsername = uiState.replyToComment?.user?.username,
            focusRequester = focusRequester
        )
    }
}

private fun sharePost(context: Context, postId: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_synapse_text, postId))
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.title_share_post)))
}

private fun shareComment(context: Context, content: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.title_share_comment)))
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun handleCommentAction(
    action: CommentAction,
    comment: CommentWithUser,
    context: Context,
    viewModel: PostDetailViewModel,
    focusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    when (action) {
        is CommentAction.Reply -> {
            viewModel.setReplyTo(comment)
            scope.launch {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
        is CommentAction.Delete -> viewModel.deleteComment(action.commentId)
        is CommentAction.Edit -> viewModel.setEditingComment(comment)
        is CommentAction.Report -> viewModel.reportComment(action.commentId, "Inappropriate", null)
        is CommentAction.Copy -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Comment", action.content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, context.getString(R.string.toast_comment_copied), Toast.LENGTH_SHORT).show()
        }
        is CommentAction.Share -> shareComment(context, action.content)
        is CommentAction.Hide -> viewModel.hideComment(action.commentId)
        is CommentAction.Pin -> viewModel.pinComment(action.commentId)
    }
}
