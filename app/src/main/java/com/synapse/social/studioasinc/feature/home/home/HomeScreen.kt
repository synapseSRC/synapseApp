package com.synapse.social.studioasinc.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.navigation.HomeDestinations
import com.synapse.social.studioasinc.ui.navigation.HomeNavGraph
import com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager
import com.synapse.social.studioasinc.feature.shared.reels.components.UploadProgressOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    reelUploadManager: ReelUploadManager,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    onNavigateToStoryViewer: (String) -> Unit,
    onNavigateToCreateReel: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPostDetail = currentDestination?.hasRoute<HomeDestinations.PostDetail>() == true
    val isFeedScreen = currentDestination?.hasRoute<HomeDestinations.Feed>() == true

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    val isBottomBarVisible = (scrollBehavior.state.collapsedFraction < 0.5f) && !isPostDetail

    val navBarTranslationY by animateFloatAsState(
        targetValue = if (isBottomBarVisible) 0f else 1f,
        label = "NavBarAnimation"
    )

    val userAvatarUrl by viewModel.userAvatarUrl.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = if (isPostDetail) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = if (isPostDetail) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
            topBar = {
                if (!isPostDetail) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                            )
                        },
                        actions = {

                            androidx.compose.material3.FilledTonalIconButton(
                                onClick = { onNavigateToCreatePost(null) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddBox,
                                    contentDescription = stringResource(R.string.create_post)
                                )
                            }


                            IconButton(onClick = onNavigateToSearch) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                            IconButton(onClick = onNavigateToInbox) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.inbox)
                                )
                            }


                            if (userAvatarUrl != null) {
                                com.synapse.social.studioasinc.ui.components.CircularAvatar(
                                    imageUrl = userAvatarUrl,
                                    contentDescription = stringResource(R.string.profile),
                                    size = 28.dp,
                                    modifier = Modifier.padding(start = Spacing.ExtraSmall, end = Spacing.Small),
                                    onClick = { onNavigateToProfile("me") }
                                )
                            } else {
                                IconButton(
                                    onClick = { onNavigateToProfile("me") },
                                    modifier = Modifier.padding(start = Spacing.ExtraSmall, end = Spacing.Small)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = stringResource(R.string.profile)
                                    )
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { innerPadding ->
            HomeNavGraph(
                navController = navController,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToEditPost = { postId -> onNavigateToCreatePost(postId) },
                onNavigateToStoryViewer = onNavigateToStoryViewer,
                onNavigateToCreateReel = onNavigateToCreateReel,
                onNavigateToCreatePost = { onNavigateToCreatePost(null) },
                modifier = Modifier.padding(innerPadding),
                bottomPadding = 80.dp
            )
        }


        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = navBarTranslationY * size.height
                }
        ) {
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Feed>() } == true,
                onClick = {
                    navController.navigate(HomeDestinations.Feed) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Feed>() } == true) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.home)
                    )
                },
                label = { Text(stringResource(R.string.home)) }
            )

            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Reels>() } == true,
                onClick = {
                    navController.navigate(HomeDestinations.Reels) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Reels>() } == true) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                        contentDescription = stringResource(R.string.reels)
                    )
                },
                label = { Text(stringResource(R.string.reels)) }
            )

            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Notifications>() } == true,
                onClick = {
                    navController.navigate(HomeDestinations.Notifications) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    BadgedBox(
                        badge = { }
                    ) {
                        Icon(
                            imageVector = if (currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Notifications>() } == true) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications)
                        )
                    }
                },
                label = { Text(stringResource(R.string.notifications)) }
            )
        }


        UploadProgressOverlay(
            uploadManager = reelUploadManager,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        )
    }
}
