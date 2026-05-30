package com.synapse.social.studioasinc.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
import com.synapse.social.studioasinc.feature.shared.theme.DarkPrimary
import com.synapse.social.studioasinc.feature.shared.theme.glassmorphic
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
            floatingActionButton = {
                if (!isPostDetail && isFeedScreen) {
                    with(sharedTransitionScope) {
                        FloatingActionButton(
                            onClick = { onNavigateToCreatePost(null) },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(bottom = if (isBottomBarVisible) 0.dp else Sizes.HeightMedium)
                                .sharedBounds(
                                    rememberSharedContentState(key = "create_post_fab"),
                                    animatedVisibilityScope = animatedVisibilityScope
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
            },
            topBar = {
                if (!isPostDetail) {
                    TopAppBar(
                        modifier = Modifier.glassmorphic(blurRadius = 40f),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        ),
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
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
                }
            }
        ) { innerPadding ->
            HomeNavGraph(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
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
                bottomPadding = Sizes.HeightLarge
            )
        }


        val infiniteTransition = rememberInfiniteTransition(label = "NavGlow")
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowScale"
        )
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )

        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = navBarTranslationY * size.height
                }
                .glassmorphic(blurRadius = 40f)
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
                    val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Feed>() } == true
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.home),
                        modifier = if (isSelected) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = DarkPrimary.copy(alpha = glowAlpha),
                                    radius = size.maxDimension * 0.8f * glowScale
                                )
                            }
                        } else Modifier
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
                    val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Reels>() } == true
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                        contentDescription = stringResource(R.string.reels),
                        modifier = if (isSelected) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = DarkPrimary.copy(alpha = glowAlpha),
                                    radius = size.maxDimension * 0.8f * glowScale
                                )
                            }
                        } else Modifier
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
                    val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<HomeDestinations.Notifications>() } == true
                    BadgedBox(
                        badge = { }
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            modifier = if (isSelected) {
                                Modifier.drawBehind {
                                    drawCircle(
                                        color = DarkPrimary.copy(alpha = glowAlpha),
                                        radius = size.maxDimension * 0.8f * glowScale
                                    )
                                }
                            } else Modifier
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
                .padding(top = Sizes.Height100)
        )
    }
}
