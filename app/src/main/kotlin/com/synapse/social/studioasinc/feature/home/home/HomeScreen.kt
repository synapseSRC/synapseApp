package com.synapse.social.studioasinc.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
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
import com.synapse.social.studioasinc.ui.settings.NavigationPlacement
import com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager
import com.synapse.social.studioasinc.feature.shared.reels.components.UploadProgressOverlay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    reelUploadManager: ReelUploadManager,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToCreatePost: (String?) -> Unit,
    onNavigateToQuotePost: (String) -> Unit,
    onNavigateToStoryViewer: (String, List<String>) -> Unit,
    onNavigateToCreateReel: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPostDetail = currentDestination?.hasRoute<HomeDestinations.PostDetail>() == true
    val isFeedScreen = currentDestination?.hasRoute<HomeDestinations.Feed>() == true

    val navigationPlacement by viewModel.navigationPlacement.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val isBottomMode = navigationPlacement == NavigationPlacement.BOTTOM
    val isBottomBarVisible = isBottomMode && (scrollBehavior.state.collapsedFraction < 0.5f) && !isPostDetail

    val navBarTranslationY by animateFloatAsState(
        targetValue = if (isBottomBarVisible) 0f else 1f,
        label = "NavBarAnimation"
    )

    val userAvatarUrl by viewModel.userAvatarUrl.collectAsStateWithLifecycle()

    val isFeedSelected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Feed>() } == true
    val isReelsSelected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Reels>() } == true
    val isNotificationsSelected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Notifications>() } == true

    val selectedTabIndex = when {
        isReelsSelected -> 1
        isNotificationsSelected -> 2
        else -> 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = if (isPostDetail) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = if (isPostDetail) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
            floatingActionButton = {},
            topBar = {
                if (!isPostDetail) {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            actions = {
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
                                        size = Sizes.AvatarTiny,
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

                        if (!isBottomMode) {
                            PrimaryTabRow(
                                selectedTabIndex = selectedTabIndex,
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Tab(
                                    selected = isFeedSelected,
                                    onClick = {
                                        navController.navigate(HomeDestinations.Feed) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    text = { Text(stringResource(R.string.home)) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isFeedSelected) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = stringResource(R.string.home)
                                        )
                                    }
                                )
                                Tab(
                                    selected = isReelsSelected,
                                    onClick = {
                                        navController.navigate(HomeDestinations.Reels) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    text = { Text(stringResource(R.string.reels)) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isReelsSelected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                                            contentDescription = stringResource(R.string.reels)
                                        )
                                    }
                                )
                                Tab(
                                    selected = isNotificationsSelected,
                                    onClick = {
                                        navController.navigate(HomeDestinations.Notifications) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    text = { Text(stringResource(R.string.notifications)) },
                                    icon = {
                                        BadgedBox(
                                            badge = { }
                                        ) {
                                            Icon(
                                                imageVector = if (isNotificationsSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                                contentDescription = stringResource(R.string.notifications)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            HomeNavGraph(
                navController = navController,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToQuotePost = onNavigateToQuotePost,
                onNavigateToEditPost = { postId -> onNavigateToCreatePost(postId) },
                onNavigateToStoryViewer = { userId, allUserIds ->
                    onNavigateToStoryViewer(userId, allUserIds)
                },
                onNavigateToCreateReel = onNavigateToCreateReel,
                onNavigateToCreatePost = { onNavigateToCreatePost(null) },
                modifier = Modifier.padding(innerPadding),
                bottomPadding = if (isBottomMode) Sizes.HeightLarge else Spacing.None
            )
        }

        if (!isPostDetail && isFeedScreen) {
            with(sharedTransitionScope) {
                val fabBoundsTransform = remember {
                    androidx.compose.animation.BoundsTransform { _, _ ->
                        androidx.compose.animation.core.spring(
                            dampingRatio = 0.8f,
                            stiffness = 380f
                        )
                    }
                }
                val fabOverlayClip = remember { OverlayClip(CircleShape) }

                FloatingActionButton(
                    onClick = { onNavigateToCreatePost(null) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = Spacing.Medium,
                            bottom = if (isBottomMode) Sizes.HeightLarge else Spacing.Medium
                        )
                        .graphicsLayer {
                            translationY = if (isBottomMode) navBarTranslationY * size.height else 0f
                        }
                        .sharedBounds(
                            rememberSharedContentState(key = "create_post_fab"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = fabBoundsTransform,
                            clipInOverlayDuringTransition = fabOverlayClip
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_post),
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "create_post_icon"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                }
            }
        }

        if (isBottomMode) {
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = navBarTranslationY * size.height
                    }
            ) {
                NavigationBarItem(
                    selected = isFeedSelected,
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
                            imageVector = if (isFeedSelected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = stringResource(R.string.home)
                        )
                    },
                    label = { Text(stringResource(R.string.home)) }
                )

                NavigationBarItem(
                    selected = isReelsSelected,
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
                            imageVector = if (isReelsSelected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                            contentDescription = stringResource(R.string.reels)
                        )
                    },
                    label = { Text(stringResource(R.string.reels)) }
                )

                NavigationBarItem(
                    selected = isNotificationsSelected,
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
                                imageVector = if (isNotificationsSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications)
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.notifications)) }
                )
            }
        }

        UploadProgressOverlay(
            uploadManager = reelUploadManager,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Sizes.Height100)
        )
    }
}
