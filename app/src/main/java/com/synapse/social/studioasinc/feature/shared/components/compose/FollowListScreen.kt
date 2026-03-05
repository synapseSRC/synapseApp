package com.synapse.social.studioasinc.feature.shared.components.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.feature.shared.components.compose.components.UserListItem
import com.synapse.social.studioasinc.viewmodel.FollowListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FollowListScreen(
    userId: String,
    listType: String,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val initialPage = if (listType == "following") 1 else 0
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 2 }
    )

    LaunchedEffect(userId) {
        viewModel.loadUsers(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (pagerState.currentPage == 0) {
                            stringResource(R.string.followers)
                        } else {
                            stringResource(R.string.following)
                        }
                    )
                },
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
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.followers),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.following),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val listState = if (page == 0) uiState.followers else uiState.following
                val emptyMessage = if (page == 0) stringResource(R.string.no_followers) else stringResource(R.string.no_following)

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        listState.isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        listState.error != null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = listState.error ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        listState.users.isEmpty() -> {
                            Text(
                                text = emptyMessage,
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(listState.users, key = { it.uid }) { user ->
                                    UserListItem(
                                        user = user,
                                        onUserClick = { onUserClick(user.uid) },
                                        onMessageClick = { onMessageClick(user.uid) }
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
