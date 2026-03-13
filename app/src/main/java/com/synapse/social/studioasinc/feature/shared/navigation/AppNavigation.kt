package com.synapse.social.studioasinc.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.synapse.social.studioasinc.core.auth.AuthHelper
import com.synapse.social.studioasinc.feature.auth.ui.AuthScreen
import com.synapse.social.studioasinc.ui.home.HomeScreen
import com.synapse.social.studioasinc.feature.profile.profile.ProfileScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.InboxScreen
import com.synapse.social.studioasinc.ui.search.SearchScreen
import com.synapse.social.studioasinc.ui.search.SearchViewModel
import com.synapse.social.studioasinc.feature.post.postdetail.PostDetailScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostViewModel
import com.synapse.social.studioasinc.ui.settings.SettingsScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.feature.profile.editprofile.RegionSelectionScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoHistoryScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoType
import com.synapse.social.studioasinc.feature.shared.components.compose.FollowListScreen
import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerScreen
import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerViewModel
import com.synapse.social.studioasinc.feature.stories.creator.StoryCreatorActivity
import com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager
import com.synapse.social.studioasinc.feature.profile.profile.ProfileViewModel
import com.synapse.social.studioasinc.ui.settings.SettingsNavHost
import kotlinx.serialization.Serializable
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.ChatScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Any = AppDestination.Auth,
    reelUploadManager: ReelUploadManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        authGraph(navController)
        homeGraph(navController, reelUploadManager)
        inboxGraph(navController)
        postGraph(navController)
        profileGraph(navController)
        storyGraph(navController)

    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    composable<AppDestination.Auth> {
                val viewModel: com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.SignInViewModel = hiltViewModel()
                AuthScreen(
                    signInViewModel = viewModel,
                    onInitiateGoogleSignIn = {
                        // Usually handled by Activity; here just placeholder or delegate to viewModel
                    },
                    onNavigateToMain = {
                        navController.navigate(AppDestination.Home) {
                            popUpTo(AppDestination.Auth) { inclusive = true }
                        }
                    }
                )
            }
}

fun NavGraphBuilder.homeGraph(navController: NavHostController, reelUploadManager: ReelUploadManager) {
    composable<AppDestination.Home> {
                HomeScreen(
                    reelUploadManager = reelUploadManager,
                    onNavigateToSearch = {
                        navController.navigate(AppDestination.Search)
                    },
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
                    },
                    onNavigateToInbox = {
                        try {
                            navController.navigate(AppDestination.Inbox)
                        } catch (e: IllegalArgumentException) {
                            // Handle error
                        }
                    },
                    onNavigateToCreatePost = { postId ->
                        navController.navigate(AppDestination.CreatePost(postId = postId))
                    },
                    onNavigateToStoryViewer = { userId ->
                        navController.navigate(AppDestination.StoryViewer(userId))
                    },
                    onNavigateToCreateReel = {
                        navController.navigate(AppDestination.CreatePost(type = "reel"))
                    }
                )
            }
    composable<AppDestination.Search> {
                val viewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
                    },
                    onNavigateToPost = { postId ->
                        navController.navigate(AppDestination.PostDetail(postId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
}

fun NavGraphBuilder.inboxGraph(navController: NavHostController) {
    composable<AppDestination.Inbox> {
                InboxScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
                    },
                    onNavigateToChat = { chatId, userId, userName, avatar ->
                        navController.navigate(AppDestination.Chat(chatId, userId, userName, avatar))
                    },
                    onNavigateToCreateGroup = { navController.navigate(AppDestination.CreateGroup) }
                )
            }
    composable<AppDestination.Chat> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.Chat>()
                ChatScreen(
                    chatId = args.chatId,
                    participantId = args.userId,
                    initialParticipantName = args.participantName,
                    initialParticipantAvatar = args.participantAvatar,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGroupInfo = { chatId, groupName ->
                        navController.navigate(AppDestination.GroupInfo(chatId, groupName))
                    }
                )
            }
    composable<AppDestination.CreateGroup> {
                com.synapse.social.studioasinc.feature.inbox.inbox.CreateGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupCreated = { chatId ->
                        navController.popBackStack()
                        navController.navigate(AppDestination.Chat(chatId = chatId))
                    }
                )
            }
    composable<AppDestination.GroupInfo> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.GroupInfo>()
                com.synapse.social.studioasinc.feature.inbox.inbox.GroupInfoScreen(
                    chatId = args.chatId,
                    groupName = args.groupName,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
}

fun NavGraphBuilder.profileGraph(navController: NavHostController) {
    composable<AppDestination.Profile>(
                deepLinks = listOf(navDeepLink<AppDestination.Profile>(basePath = "synapse://profile"))
            ) { backStackEntry ->
                val context = LocalContext.current
                val args = backStackEntry.toRoute<AppDestination.Profile>()
                val userId = args.userId
                val currentUserId = AuthHelper.getCurrentUserId() ?: return@composable
                val targetUserId = if (userId == "me") currentUserId else userId
                val viewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    userId = targetUserId,
                    currentUserId = currentUserId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditProfile = {
                        navController.navigate(AppDestination.EditProfile)
                    },
                    onNavigateToEditPost = { postId ->
                        navController.navigate(AppDestination.CreatePost(postId = postId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(AppDestination.Settings)
                    },
                    onNavigateToChat = { targetUserId, userName ->
                        navController.navigate(AppDestination.Chat(chatId = "new", userId = targetUserId, participantName = userName))
                    },
                    onNavigateToFollowers = {
                        navController.navigate(AppDestination.FollowList(userId, "followers"))
                    },
                    onNavigateToFollowing = {
                        navController.navigate(AppDestination.FollowList(userId, "following"))
                    },
                    onNavigateToUserProfile = { targetUid ->
                        navController.navigate(AppDestination.Profile(targetUid))
                    },
                    onNavigateToStoryCreator = {
                        context.startActivity(Intent(context, StoryCreatorActivity::class.java))
                    },
                    viewModel = viewModel
                )
            }
    composable<AppDestination.EditProfile> {
                val viewModel: EditProfileViewModel = hiltViewModel()

                val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                val selectedRegion = savedStateHandle?.get<String>("selected_region")

                LaunchedEffect(selectedRegion) {
                    selectedRegion?.let { region ->
                        viewModel.onEvent(EditProfileEvent.RegionSelected(region))
                        savedStateHandle.remove<String>("selected_region")
                    }
                }

                EditProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRegionSelection = { _ ->
                        navController.navigate(AppDestination.RegionSelection)
                    },
                    onNavigateToPhotoHistory = { type ->
                        navController.navigate(AppDestination.PhotoHistory(type))
                    }
                )
            }
    composable<AppDestination.RegionSelection> {
                RegionSelectionScreen(
                    onBackClick = { navController.popBackStack() },
                    onRegionSelected = { region ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_region", region)
                        navController.popBackStack()
                    }
                )
            }
    composable<AppDestination.PhotoHistory> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.PhotoHistory>()
                val typeStr = args.type
                val photoType = try {
                    PhotoType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    return@composable
                }

                PhotoHistoryScreen(
                    type = photoType,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
    composable<AppDestination.FollowList> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.FollowList>()
                val userId = args.userId
                val currentUserId = AuthHelper.getCurrentUserId() ?: return@composable
                val targetUserId = if (userId == "me") currentUserId else userId
                val listType = args.type
                FollowListScreen(
                    userId = targetUserId,
                    listType = listType,
                    onNavigateBack = { navController.popBackStack() },
                    onUserClick = { profileUserId ->
                        navController.navigate(AppDestination.Profile(profileUserId))
                    },
                    onMessageClick = { userId, userName ->
                        navController.navigate(AppDestination.Chat(chatId = "new", userId = userId, participantName = userName))
                    }
                )
            }
    composable<AppDestination.Settings> {
                SettingsNavHost(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToProfileEdit = {
                        navController.navigate(AppDestination.EditProfile)
                    },
                    onNavigateToChatPrivacy = {
                        navController.navigate(AppDestination.ChatPrivacy)
                    },
                    onLogout = {
                        navController.navigate(AppDestination.Auth) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
    composable<AppDestination.ChatPrivacy> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chat Privacy Settings (To be implemented)")
                }
            }
}

fun NavGraphBuilder.postGraph(navController: NavHostController) {
    composable<AppDestination.CreatePost> { backStackEntry ->
                val viewModel: CreatePostViewModel = hiltViewModel()
                val args = backStackEntry.toRoute<AppDestination.CreatePost>()
                val postId = args.postId
                val type = args.type

                LaunchedEffect(postId, type) {
                    viewModel.setCompositionType(type)
                    if (postId != null) {
                        viewModel.loadPostForEdit(postId)
                    }
                }

                CreatePostScreen(
                    viewModel = viewModel,
                    onNavigateUp = { navController.popBackStack() }
                )
            }
    composable<AppDestination.PostDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.PostDetail>()
                PostDetailScreen(
                    postId = args.postId,
                    rootCommentId = args.commentId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
                    },
                    onNavigateToEditPost = { pid ->
                        navController.navigate(AppDestination.CreatePost(pid))
                },
                onNavigateToCommentDetail = { postId, commentId ->
                    navController.navigate(AppDestination.PostDetail(postId, commentId))
                    }
                )
            }
    composable<AppDestination.QuotePost> {
                val viewModel: com.synapse.social.studioasinc.feature.createpost.quote.QuotePostViewModel = hiltViewModel()
                com.synapse.social.studioasinc.feature.createpost.quote.QuotePostScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
}

fun NavGraphBuilder.storyGraph(navController: NavHostController) {
    composable<AppDestination.StoryViewer> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.StoryViewer>()
                val userId = args.userId
                val viewModel: StoryViewerViewModel = hiltViewModel()

                LaunchedEffect(userId) {
                    viewModel.loadStories(userId)
                }

                StoryViewerScreen(
                    onClose = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
}
