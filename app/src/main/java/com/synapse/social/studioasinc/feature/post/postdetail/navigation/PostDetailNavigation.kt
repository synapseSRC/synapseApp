package com.synapse.social.studioasinc.feature.post.postdetail.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.synapse.social.studioasinc.feature.post.postdetail.PostDetailScreen

const val postDetailRoute = "post_detail/{postId}?commentId={commentId}"

fun NavController.navigateToPostDetail(postId: String, commentId: String? = null) {
    if (commentId != null) {
        this.navigate("post_detail/$postId?commentId=$commentId")
    } else {
        this.navigate("post_detail/$postId")
    }
}

fun NavGraphBuilder.postDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToEditPost: (String) -> Unit,
    onNavigateToReplyToPost: (String) -> Unit,
    onNavigateToCommentDetail: (String, String) -> Unit
) {
    composable(
        route = postDetailRoute,
        arguments = listOf(
            navArgument("postId") { type = NavType.StringType },
            navArgument("commentId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
        val commentId = backStackEntry.arguments?.getString("commentId")
        PostDetailScreen(
            postId = postId,
            rootCommentId = commentId,
            onNavigateBack = onNavigateBack,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToEditPost = onNavigateToEditPost,
            onNavigateToReplyToPost = onNavigateToReplyToPost,
            onNavigateToCommentDetail = onNavigateToCommentDetail
        )
    }
}
