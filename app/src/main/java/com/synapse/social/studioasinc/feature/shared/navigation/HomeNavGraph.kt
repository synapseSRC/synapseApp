package com.synapse.social.studioasinc.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.synapse.social.studioasinc.ui.home.FeedScreen
import com.synapse.social.studioasinc.feature.shared.reels.ReelsScreen
import com.synapse.social.studioasinc.ui.notifications.NotificationsScreen
import com.synapse.social.studioasinc.feature.post.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.feature.stories.creator.StoryCreatorActivity
import kotlinx.serialization.Serializable

sealed interface HomeDestinations {
    @Serializable
    data object Feed : HomeDestinations

    @Serializable
    data object Reels : HomeDestinations

    @Serializable
    data object Notifications : HomeDestinations

    @Serializable
    data class PostDetail(val postId: String, val commentId: String? = null) : HomeDestinations

    @Serializable
    data class Profile(val userId: String) : HomeDestinations

    @Serializable
    data object CreateReelPlaceholder : HomeDestinations
}

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToEditPost: (String) -> Unit,
    onNavigateToStoryViewer: (String) -> Unit = {},
    onNavigateToCreateReel: () -> Unit = {},
    onNavigateToCreatePost: () -> Unit = {},
    startDestination: Any = HomeDestinations.Feed,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<HomeDestinations.Feed> {
            FeedScreen(
                onPostClick = { postId -> navController.navigate(HomeDestinations.PostDetail(postId)) },
                onUserClick = { userId -> onNavigateToProfile(userId) },
                onCommentClick = { postId -> navController.navigate(HomeDestinations.PostDetail(postId)) },
                onQuoteClick = { postId -> navController.navigate(AppDestination.QuotePost(postId)) },
                onMediaClick = { },
                onEditPost = onNavigateToEditPost,
                onStoryClick = { userId ->
                    onNavigateToStoryViewer(userId)
                },
                onAddStoryClick = {
                    context.startActivity(Intent(context, StoryCreatorActivity::class.java))
                },
                onCreatePostClick = onNavigateToCreatePost,
                contentPadding = PaddingValues(bottom = bottomPadding)
            )
        }

        composable<HomeDestinations.Reels> {
             ReelsScreen(
                 onUserClick = { userId -> onNavigateToProfile(userId) },
                 onCommentClick = { },
                 onBackClick = { navController.popBackStack() },
                 contentPadding = PaddingValues(bottom = bottomPadding)
             )
        }

        composable<HomeDestinations.Notifications> {
             NotificationsScreen(
                 onNotificationClick = { notification -> },
                 onUserClick = { userId -> onNavigateToProfile(userId) },
                 contentPadding = PaddingValues(bottom = bottomPadding)
             )
        }

        composable<HomeDestinations.PostDetail> { backStackEntry ->
             val args = backStackEntry.toRoute<HomeDestinations.PostDetail>()
             PostDetailScreen(
                 postId = args.postId,
                 rootCommentId = args.commentId,
                 onNavigateBack = { navController.popBackStack() },
                 onNavigateToProfile = onNavigateToProfile,
                 onNavigateToEditPost = onNavigateToEditPost,
                 onNavigateToReplyToPost = { pid ->
                     navController.navigate(AppDestination.CreatePost(replyToPostId = pid))
                 },
                 onNavigateToCommentDetail = { postId, commentId ->
                     navController.navigate(HomeDestinations.PostDetail(postId, commentId))
                 }
             )
        }

        composable<HomeDestinations.CreateReelPlaceholder> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            LaunchedEffect(Unit) {
                onNavigateToCreateReel()
                navController.popBackStack()
            }
        }
    }
}
