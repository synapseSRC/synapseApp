package com.synapse.social.studioasinc.feature.shared.components.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.compose.components.UserListItem
import com.synapse.social.studioasinc.viewmodel.FollowListViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListScreen(
    userId: String,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onMessageClick: (String, String?, String?) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.followers),
        stringResource(R.string.following)
    )

    var hasLoadedFollowers by remember { mutableStateOf(false) }
    var hasLoadedFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(userId, pagerState.currentPage) {
        if (pagerState.currentPage == 0 && !hasLoadedFollowers) {
            viewModel.loadFollowers(userId)
            hasLoadedFollowers = true
        } else if (pagerState.currentPage == 1 && !hasLoadedFollowing) {
            viewModel.loadFollowing(userId)
            hasLoadedFollowing = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.users)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val isLoading = if (page == 0) uiState.followersLoading else uiState.followingLoading
                val error = if (page == 0) uiState.followersError else uiState.followingError
                val users = if (page == 0) uiState.followers else uiState.following
                val emptyMessage = if (page == 0) stringResource(R.string.no_followers) else stringResource(R.string.no_following)

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        error != null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = error ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        users.isEmpty() -> {
                            Text(
                                text = emptyMessage,
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(users) { user ->
                                    UserListItem(
                                        user = user,
                                        onUserClick = { onUserClick(user.uid) },
                                        onMessageClick = { onMessageClick(user.uid, user.displayName ?: user.username, user.avatar) }
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