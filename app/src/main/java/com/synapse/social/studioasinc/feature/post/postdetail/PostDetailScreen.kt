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
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToEditPost: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagingItems = viewModel.commentsPagingFlow.collectAsLazyPagingItems()

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableIntStateOf(0) }
    var showPostOptions by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    var showCommentOptions by remember { mutableStateOf<CommentWithUser?>(null) }
    var showReactionPickerForComment by remember { mutableStateOf<CommentWithUser?>(null) }

    val currentUserId = uiState.currentUserId

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(uiState.refreshTrigger) {
        if (uiState.refreshTrigger > 0) {
            pagingItems.refresh()
        }
    }

    fun sharePost() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_synapse_text, postId))
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.title_share_post)))
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
            onShare = { sharePost() },
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
                    is CommentAction.Share -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, action.content)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.title_share_comment)))
                    }
                    is CommentAction.Hide -> viewModel.hideComment(action.commentId)
                    is CommentAction.Pin -> viewModel.pinComment(action.commentId)
                }
                showCommentOptions = null
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
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
        },
        bottomBar = {
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
                        onCancelReply = { viewModel.setReplyTo(null) }
                    )
                }
                if (uiState.editingComment != null) {
                    EditIndicator(
                        comment = uiState.editingComment!!,
                        onCancel = { viewModel.setEditingComment(null) }
                    )
                }
                CommentInput(
                    onSend = {
                        if (uiState.editingComment != null) {
                            viewModel.editComment(uiState.editingComment!!.id, it)
                        } else {
                            viewModel.addComment(it)
                        }
                    },
                    initialValue = uiState.editingComment?.content ?: "",
                    focusRequester = focusRequester
                )
            }
        }
    ) { paddingValues ->
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
                        onReplyClick = {
                            viewModel.setReplyTo(it)
                            scope.launch {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        },
                        onLikeClick = { viewModel.toggleCommentReaction(it, ReactionType.LIKE) },
                        onShowReactions = { showReactionPickerForComment = it },
                        onShowOptions = { showCommentOptions = it },
                        onUserClick = onNavigateToProfile,
                        onViewReplies = { commentId: String -> viewModel.loadReplies(commentId) },
                        modifier = Modifier.fillMaxSize(),
                        headerContent = {
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

                            val actions = remember(viewModel, context, postId) {
                                PostActions(
                                    onLike = { viewModel.toggleReaction(ReactionType.LIKE) },
                                    onComment = {
                                        scope.launch {
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        }
                                    },
                                    onShare = { sharePost() },
                                    onRepost = { viewModel.createReshare(null) },
                                    onBookmark = { viewModel.toggleBookmark() },
                                    onOptionClick = { showPostOptions = true },
                                    onPollVote = { _, index -> viewModel.votePoll(index) },
                                    onUserClick = { uid -> onNavigateToProfile(uid) },
                                    onMediaClick = { index ->
                                        selectedMediaIndex = index
                                        showMediaViewer = true
                                    },
                                    onReactionSelected = { _, reaction -> viewModel.toggleReaction(reaction) }
                                )
                            }

                            SharedPostItem(
                                post = mergedPost,
                                actions = actions,
                                isExpanded = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }
            }
        }
    }
}
