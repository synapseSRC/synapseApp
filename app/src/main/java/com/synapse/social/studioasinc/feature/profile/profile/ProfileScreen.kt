package com.synapse.social.studioasinc.feature.profile.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.feature.shared.components.post.PostActionsFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.feature.post.PostDetailActivity
import com.synapse.social.studioasinc.ui.components.EmptyState
import com.synapse.social.studioasinc.ui.components.ErrorState
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.feature.shared.components.MediaViewer
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.feature.profile.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.feature.profile.profile.components.*
import com.synapse.social.studioasinc.feature.profile.profile.components.UserSearchDialog
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.delay



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToEditPost: (String) -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val effectiveIsOwnProfile = state.isOwnProfile && state.viewAsMode == null

    val effectiveState = state.copy(isOwnProfile = effectiveIsOwnProfile)

    val listState = rememberLazyListState()
    var showUserSearchDialog by remember { mutableStateOf(false) }

    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialMediaPage by remember { mutableStateOf(0) }

    var showPostOptions by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    // Handle block success/error messages
    LaunchedEffect(state.blockSuccess, state.blockError) {
        when {
            state.blockSuccess -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.block_success)
                )
                viewModel.clearBlockStatus()
            }
            state.blockError != null -> {
                snackbarHostState.showSnackbar(
                    message = state.blockError ?: context.getString(R.string.error_block_failed)
                )
                viewModel.clearBlockStatus()
            }
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val coverHeightPx = with(density) { 200.dp.toPx() }

    val scrollProgress = remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / coverHeightPx).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { isTraversalGroup = true }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = {
                    viewModel.refreshProfile(userId)
                },
                modifier = Modifier.fillMaxSize()
            ) {
            when (val profileState = state.profileState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 100.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator()
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        state = effectiveState,
                        profile = profileState.profile,
                        listState = listState,
                        scrollProgress = scrollProgress.value,
                        viewModel = viewModel,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToFollowing = onNavigateToFollowing,
                        onNavigateToUserProfile = onNavigateToUserProfile,
                        onNavigateToChat = onNavigateToChat,
                        onCustomizeClick = { },
                        onOpenMediaViewer = { urls, index ->
                            selectedMediaUrls = urls
                            initialMediaPage = index
                            showMediaViewer = true
                        },
                        onShowPostOptions = { post ->
                            selectedPost = post
                            showPostOptions = true
                        }
                    )
                }
                is ProfileUiState.Error -> {
                    ErrorState(
                        title = stringResource(R.string.error_loading_profile),
                        message = profileState.message,
                        onRetry = { viewModel.refreshProfile(userId) }
                    )
                }
                is ProfileUiState.Empty -> {
                    EmptyState(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.no_user_data_found),
                        message = stringResource(R.string.profile_not_found_msg)
                    )
                }
            }
        }

        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ProfileTopAppBar(
            displayName = profile?.name ?: profile?.username ?: "",
            scrollProgress = scrollProgress.value,
            onBackClick = onNavigateBack,
            onMoreClick = { viewModel.toggleMoreMenu() }
        )
    }
}


    if (state.showMoreMenu) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ProfileMoreMenuBottomSheet(
            isOwnProfile = effectiveIsOwnProfile,
            onDismiss = { viewModel.toggleMoreMenu() },
            onShareProfile = { viewModel.showShareSheet() },
            onViewAs = { viewModel.showViewAsSheet() },
            onLockProfile = {
                profile?.let { viewModel.lockProfile(!it.isPrivate) }
            },
            onArchiveProfile = {
                profile?.let { viewModel.archiveProfile(true) }
            },
            onQrCode = { viewModel.showQrCode() },
            onCopyLink = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Profile Link", url)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, context.getString(R.string.toast_link_copied), android.widget.Toast.LENGTH_SHORT).show()
            },
            onSettings = onNavigateToSettings,
            onActivityLog = onNavigateToActivityLog,
            onBlockUser = {
                profile?.let { viewModel.blockUser(it.id) }
            },
            onReportUser = { viewModel.showReportDialog() },
            onMuteUser = {
                profile?.let { viewModel.muteUser(it.id) }
            }
        )
    }

    if (state.showShareSheet) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ShareProfileBottomSheet(
            onDismiss = { viewModel.hideShareSheet() },
            onCopyLink = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Profile Link", url)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, context.getString(R.string.toast_link_copied), android.widget.Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareToStory = {
                Toast.makeText(context, context.getString(R.string.toast_share_story_soon), Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareViaMessage = {
                Toast.makeText(context, context.getString(R.string.toast_share_message_soon), Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareExternal = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_profile_text, url))
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_profile)))
                viewModel.hideShareSheet()
            }
        )
    }

    if (state.showViewAsSheet) {
        ViewAsBottomSheet(
            onDismiss = { viewModel.hideViewAsSheet() },
            onViewAsPublic = {
                viewModel.setViewAsMode(ViewAsMode.PUBLIC)
                viewModel.hideViewAsSheet()
            },
            onViewAsFriends = {
                viewModel.setViewAsMode(ViewAsMode.FRIENDS)
                viewModel.hideViewAsSheet()
            },
            onViewAsSpecificUser = {
                showUserSearchDialog = true
                viewModel.hideViewAsSheet()
            }
        )
    }

    if (showUserSearchDialog) {
        UserSearchDialog(
            onDismiss = {
                showUserSearchDialog = false
                viewModel.clearSearchResults()
            },
            onUserSelected = { user ->
                showUserSearchDialog = false
                viewModel.clearSearchResults()
                viewModel.setViewAsMode(ViewAsMode.SPECIFIC_USER, user.username ?: context.getString(R.string.default_user_name))
            },
            onSearch = { query ->
                viewModel.searchUsers(query)
            },
            searchResults = state.searchResults,
            isSearching = state.isSearching
        )
    }

    if (state.showQrCode) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        QRCodeDialog(
            profileUrl = "${BuildConfig.APP_DOMAIN}/profile/${profile?.username ?: ""}",
            username = profile?.username ?: "",
            onDismiss = { viewModel.hideQrCode() }
        )
    }

    if (state.showReportDialog) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        profile?.let {
            ReportUserDialog(
                username = it.username,
                onDismiss = { viewModel.hideReportDialog() },
                onReport = { reason -> viewModel.reportUser(it.id, reason) }
            )
        }
    }


    if (showMediaViewer) {
        MediaViewer(
            mediaUrls = selectedMediaUrls,
            initialPage = initialMediaPage,
            onDismiss = { showMediaViewer = false }
        )
    }


    if (showPostOptions && selectedPost != null) {
        val post = selectedPost!!
        PostOptionsBottomSheet(
            post = post,
            isOwner = (post.authorUid == currentUserId) && (state.viewAsMode == null),
            commentsDisabled = post.postDisableComments == "true",
            onDismiss = {
                showPostOptions = false
                selectedPost = null
            },
            onEdit = {
                showPostOptions = false
                selectedPost = null
                onNavigateToEditPost(post.id)
            },
            onDelete = {
                viewModel.deletePost(post.id)
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_text, "${BuildConfig.APP_DOMAIN}/post/${post.id}"))
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_post)))
            },
            onCopyLink = {
                val url = "${BuildConfig.APP_DOMAIN}/post/${post.id}"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Post Link", url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            },
            onBookmark = {
                viewModel.toggleSave(post.id)
            },
            onToggleComments = {
                Toast.makeText(context, context.getString(R.string.toast_toggle_comments_impl), Toast.LENGTH_SHORT).show()
            },
            onReport = {
                viewModel.reportPost(post.id, context.getString(R.string.toast_reported_from_profile))
                Toast.makeText(context, context.getString(R.string.toast_report_submitted), Toast.LENGTH_SHORT).show()
            },
            onBlock = {
                viewModel.blockUser(post.authorUid)
            },
            onRevokeVote = {
                Toast.makeText(context, context.getString(R.string.toast_revoke_vote_impl), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileScreenState,
    profile: com.synapse.social.studioasinc.data.model.UserProfile,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollProgress: Float,
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onCustomizeClick: () -> Unit = {},
    onOpenMediaViewer: (List<String>, Int) -> Unit,
    onShowPostOptions: (Post) -> Unit
) {

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "contentAlpha"
    )

    val context = LocalContext.current

    var bioExpanded by remember { mutableStateOf(false) }

    val currentOnNavigateToUserProfile by rememberUpdatedState(onNavigateToUserProfile)
    val currentOnOpenMediaViewer by rememberUpdatedState(onOpenMediaViewer)
    val currentOnShowPostOptions by rememberUpdatedState(onShowPostOptions)



    val actions = remember(context, viewModel) {
        PostActionsFactory.create(
            viewModel = viewModel,
            onComment = { post ->
                val intent = Intent(context, PostDetailActivity::class.java).apply {
                    putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
                    putExtra(PostDetailActivity.EXTRA_AUTHOR_UID, post.authorUid)
                }
                context.startActivity(intent)
            },
            onShare = { post ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_text, "${BuildConfig.APP_DOMAIN}/post/${post.id}"))
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_post)))
            },
            onUserClick = { userId -> currentOnNavigateToUserProfile(userId) },
            onOptionClick = { post -> currentOnShowPostOptions(post) },
            onMediaClick = { index -> }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
    ) {

        if (state.viewAsMode != null) {
            item {
                ViewAsBanner(
                    viewMode = state.viewAsMode,
                    specificUserName = state.viewAsUserName,
                    onExitViewAs = { viewModel.exitViewAs() }
                )
            }
        }


        item {
            ProfileHeader(
                avatar = profile.avatar,
                status = profile.status,
                coverImageUrl = profile.coverImageUrl,
                name = profile.name,
                username = profile.username,
                nickname = profile.nickname,
                bio = profile.bio,
                isVerified = profile.isVerified,
                hasStory = state.hasStory,
                postsCount = profile.postCount,
                followersCount = profile.followerCount,
                followingCount = profile.followingCount,
                isOwnProfile = state.isOwnProfile && state.viewAsMode == null,
                isFollowing = state.isFollowing,
                isFollowLoading = state.isFollowLoading,
                scrollOffset = scrollProgress,
                bioExpanded = bioExpanded,
                onToggleBio = { bioExpanded = !bioExpanded },
                onProfileImageClick = {
                     if (state.isOwnProfile) {
                         onNavigateToEditProfile()
                     } else if (!profile.avatar.isNullOrBlank()) {
                         onOpenMediaViewer(listOf(profile.avatar), 0)
                     }
                },
                onCoverPhotoClick = {
                     if (state.isOwnProfile) {
                         onNavigateToEditProfile()
                     } else if (!profile.coverImageUrl.isNullOrBlank()) {
                         onOpenMediaViewer(listOf(profile.coverImageUrl), 0)
                     }
                },
                onEditProfileClick = onNavigateToEditProfile,
                onFollowClick = {
                    if (state.isFollowing) {
                        viewModel.unfollowUser(profile.id)
                    } else {
                        viewModel.followUser(profile.id)
                    }
                },
                onMessageClick = { onNavigateToChat(profile.id) },
                onAddStoryClick = {
                    Toast.makeText(context, "Story creation coming soon", Toast.LENGTH_SHORT).show()
                },
                onMoreClick = { viewModel.toggleMoreMenu() },
                onStatsClick = { stat ->
                    when (stat) {
                        "followers" -> onNavigateToFollowers()
                        "following" -> onNavigateToFollowing()
                    }
                }
            )
        }


        item {
            Spacer(modifier = Modifier.height(8.dp))
            ContentFilterBar(
                selectedFilter = state.contentFilter,
                onFilterSelected = { filter -> viewModel.switchContentFilter(filter) },
                modifier = Modifier.fillMaxWidth(),
                showLabels = true
            )
        }


        item {
            crossfadeContent(targetState = state.contentFilter) { filter ->
                when (filter) {
                    ProfileContentFilter.PHOTOS -> {
                        if (state.photos.isEmpty() && !state.isLoadingMore) {
                            EmptyState(
                                icon = Icons.Default.PhotoLibrary,
                                title = stringResource(R.string.empty_profile_photos_title),
                                message = stringResource(R.string.empty_profile_photos_msg)
                            )
                        } else {
                            val photos = remember(state.photos) {
                                state.photos.filterIsInstance<MediaItem>()
                            }
                            PhotoGrid(
                                items = photos,
                                onItemClick = { mediaItem ->

                                    val allUrls = photos.map { it.url }
                                    val index = photos.indexOf(mediaItem)
                                    onOpenMediaViewer(allUrls, if (index >= 0) index else 0)
                                },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                    ProfileContentFilter.POSTS -> {
                        val profile = (state.profileState as? ProfileUiState.Success)?.profile ?: return@crossfadeContent
                        Column {

                            Spacer(modifier = Modifier.height(16.dp))
                            UserDetailsSection(
                                details = UserDetails(
                                    location = profile.location,
                                    joinedDate = formatJoinedDate(profile.joinedDate),
                                    relationshipStatus = profile.relationshipStatus,
                                    birthday = profile.birthday,
                                    work = profile.work,
                                    education = profile.education,
                                    website = profile.website,
                                    gender = profile.gender,
                                    pronouns = profile.pronouns,
                                    linkedAccounts = profile.linkedAccounts.map {
                                        LinkedAccount(
                                            platform = it.platform,
                                            username = it.username
                                        )
                                    }
                                ),
                                isOwnProfile = state.isOwnProfile,
                                onCustomizeClick = onCustomizeClick,
                                onWebsiteClick = { url ->
                                     try {
                                         val uri = Uri.parse(url)

                                         if (uri.scheme == "http" || uri.scheme == "https") {
                                             val intent = Intent(Intent.ACTION_VIEW, uri)
                                             context.startActivity(intent)
                                         } else {
                                             Toast.makeText(context, context.getString(R.string.error_invalid_link_scheme), Toast.LENGTH_SHORT).show()
                                         }
                                     } catch (e: Exception) {
                                         Toast.makeText(context, context.getString(R.string.error_cannot_open_link), Toast.LENGTH_SHORT).show()
                                     }
                                 },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))


                            FollowingSection(
                                users = state.followingList,
                                selectedFilter = FollowingFilter.ALL,
                                onFilterSelected = { },
                                onUserClick = { user -> onNavigateToUserProfile(user.id) },
                                onSeeAllClick = onNavigateToFollowing,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))


                            if (state.posts.isEmpty() && !state.isLoadingMore) {
                                EmptyState(
                                    icon = Icons.AutoMirrored.Filled.Article,
                                    title = if (state.isOwnProfile) stringResource(R.string.empty_own_posts_title) else stringResource(R.string.empty_user_posts_title),
                                    message = if (state.isOwnProfile) stringResource(R.string.empty_own_posts_msg) else stringResource(R.string.empty_user_posts_msg)
                                )
                            }
                        }
                    }
                    ProfileContentFilter.REELS -> {
                        if (state.reels.isEmpty() && !state.isLoadingMore) {
                            EmptyState(
                                icon = Icons.Default.VideoLibrary,
                                title = stringResource(R.string.empty_profile_reels_title),
                                message = stringResource(R.string.empty_profile_reels_msg)
                            )
                        } else {
                            val reels = remember(state.reels) {
                                state.reels.filterIsInstance<MediaItem>()
                            }
                            ReelsGrid(
                                items = reels,
                                onItemClick = {
                                    Toast.makeText(context, context.getString(R.string.toast_reels_viewer_soon), Toast.LENGTH_SHORT).show()
                                },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                }
            }
        }


        if (state.contentFilter == ProfileContentFilter.POSTS && state.posts.isNotEmpty()) {
            val posts = state.posts.filterIsInstance<com.synapse.social.studioasinc.domain.model.Post>()
            items(posts, key = { it.id }) { post ->

                val currentProfile = (state.profileState as? ProfileUiState.Success)?.profile


                val postActions = remember(actions, post) {
                    actions.copy(
                        onMediaClick = { index ->
                            val urls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)
                            if (urls.isNotEmpty()) {
                                onOpenMediaViewer(urls, index)
                            }
                        }
                    )
                }

                AnimatedPostCard(
                    post = post,
                    currentProfile = currentProfile,
                    actions = postActions
                )
            }
        }


        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}



@Composable
private fun AnimatedPostCard(
    post: com.synapse.social.studioasinc.domain.model.Post,
    currentProfile: com.synapse.social.studioasinc.data.model.UserProfile?,
    actions: PostActions
) {
    SharedPostItem(
        post = post,
        currentProfile = currentProfile,
        actions = actions
    )
}



private fun formatJoinedDate(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
    return format.format(date)
}
