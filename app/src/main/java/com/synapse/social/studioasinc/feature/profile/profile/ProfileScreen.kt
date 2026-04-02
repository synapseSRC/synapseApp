package com.synapse.social.studioasinc.feature.profile.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.synapse.social.studioasinc.feature.shared.components.MediaViewer
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostOptionsBottomSheet
import com.synapse.social.studioasinc.feature.shared.components.post.PostSummarySheet
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.feature.profile.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.feature.profile.profile.components.*
import com.synapse.social.studioasinc.feature.profile.profile.components.UserSearchDialog
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.delay
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.core.util.IntentUtils



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
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToChat: (String, String?, String?) -> Unit = { _, _, _ -> },
    onNavigateToStoryCreator: () -> Unit = {},
    onNavigateToQuotePost: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val profileLinkLabel = stringResource(R.string.clip_label_profile_link)
    val postLinkLabel = stringResource(R.string.clip_label_post_link)

    val effectiveIsOwnProfile = state.isOwnProfile && state.viewAsMode == null

    val effectiveState = state.copy(isOwnProfile = effectiveIsOwnProfile)

    val listState = rememberLazyListState()
    var showUserSearchDialog by remember { mutableStateOf(false) }

    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialMediaPage by remember { mutableStateOf(0) }

    var showPostOptions by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var showSummarySheet by remember { mutableStateOf(false) }

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
                else -> {
                    val profile = (profileState as? ProfileUiState.Success)?.profile
                    ProfileContent(
                        state = effectiveState,
                        profile = profile,
                        isLoading = profileState is ProfileUiState.Loading,
                        listState = listState,
                        scrollProgress = scrollProgress.value,
                        viewModel = viewModel,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToFollowing = onNavigateToFollowing,
                        onNavigateToQuotePost = onNavigateToQuotePost,
                        onNavigateToUserProfile = onNavigateToUserProfile,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToStoryCreator = onNavigateToStoryCreator,
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
            }
        }
        }

        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        ProfileTopAppBar(
            displayName = profile?.name ?: profile?.username ?: state.viewAsUserName ?: "",
            scrollProgress = scrollProgress.value,
            onBackClick = onNavigateBack,
            onMoreClick = { viewModel.toggleMoreMenu() }
        )

        ProfileMoreMenuSection(
            state = state,
            viewModel = viewModel,
            effectiveIsOwnProfile = effectiveIsOwnProfile,
            profile = profile,
            context = context,
            profileLinkLabel = profileLinkLabel,
            postLinkLabel = postLinkLabel,
            onNavigateToSettings = onNavigateToSettings
        )

        ShareProfileSection(
            state = state,
            viewModel = viewModel,
            profile = profile,
            context = context,
            profileLinkLabel = profileLinkLabel
        )

        ViewAsSection(
            state = state,
            viewModel = viewModel,
            showUserSearchDialog = showUserSearchDialog,
            onShowUserSearchDialog = { showUserSearchDialog = it }
        )

        UserSearchSection(
            showUserSearchDialog = showUserSearchDialog,
            viewModel = viewModel,
            state = state,
            onDismiss = { showUserSearchDialog = false }
        )

        QRCodeSection(
            state = state,
            viewModel = viewModel,
            profile = profile
        )

        ReportSection(
            state = state,
            viewModel = viewModel
        )

        MediaViewerSection(
            showMediaViewer = showMediaViewer,
            selectedMediaUrls = selectedMediaUrls,
            initialMediaPage = initialMediaPage,
            onDismiss = { showMediaViewer = false }
        )

        PostOptionsSection(
            showPostOptions = showPostOptions,
            selectedPost = selectedPost,
            currentUserId = currentUserId,
            state = state,
            viewModel = viewModel,
            context = context,
            postLinkLabel = postLinkLabel,
            onNavigateToEditPost = onNavigateToEditPost,
            onDismiss = {
                showPostOptions = false
                selectedPost = null
            }
        )
    }
}
