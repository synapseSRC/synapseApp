package com.synapse.social.studioasinc.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
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
import com.synapse.social.studioasinc.feature.profile.editprofile.media.ProfileMediaScreen
import com.synapse.social.studioasinc.feature.profile.editprofile.interestsskills.InterestsSkillsScreen
import com.synapse.social.studioasinc.feature.profile.editprofile.appearance.ProfileAppearanceScreen
import com.synapse.social.studioasinc.feature.profile.editprofile.featured.FeaturedContentScreen
import com.synapse.social.studioasinc.feature.profile.editprofile.workeducation.WorkEducationScreen
import com.synapse.social.studioasinc.feature.profile.personalinfo.PersonalInformationScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.social.SocialLinksScreen
import com.synapse.social.studioasinc.feature.profile.editprofile.RegionSelectionScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoHistoryScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoType
import com.synapse.social.studioasinc.feature.shared.components.compose.FollowListScreen
import com.synapse.social.studioasinc.feature.stories.viewer.StoryPagerScreen
import com.synapse.social.studioasinc.feature.stories.viewer.StoryViewerViewModel
import com.synapse.social.studioasinc.feature.stories.creator.StoryCreatorActivity
import com.synapse.social.studioasinc.feature.shared.reels.ReelUploadManager
import com.synapse.social.studioasinc.feature.profile.profile.ProfileViewModel
import com.synapse.social.studioasinc.feature.profile.lockprofile.LockProfileScreen
import com.synapse.social.studioasinc.feature.profile.lockprofile.LockProfileViewModel
import com.synapse.social.studioasinc.ui.settings.SettingsNavHost
import com.synapse.social.studioasinc.ui.settings.BusinessPlatformScreen
import com.synapse.social.studioasinc.ui.settings.BusinessPlatformViewModel
import com.synapse.social.studioasinc.ui.settings.ScheduledPostsScreen
import com.synapse.social.studioasinc.ui.settings.ScheduledPostsViewModel
import kotlinx.serialization.Serializable
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.ChatScreen
import androidx.compose.runtime.rememberCoroutineScope
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.core.auth.GoogleAuthHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: Any = AppDestination.Auth,
    reelUploadManager: ReelUploadManager,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            authGraph(navController, this@SharedTransitionLayout)
            homeGraph(navController, reelUploadManager, this@SharedTransitionLayout)
            inboxGraph(navController, this@SharedTransitionLayout)
            postGraph(navController, this@SharedTransitionLayout)
            profileGraph(navController, this@SharedTransitionLayout)
            storyGraph(navController, this@SharedTransitionLayout)
            hashtagGraph(navController, this@SharedTransitionLayout)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AppDestination.Auth> {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val viewModel: com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.AuthViewModel = hiltViewModel()
        AuthScreen(
            authViewModel = viewModel,
            onInitiateGoogleSignIn = {
                val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (clientId.isBlank() || clientId.contains("your-google-web-client-id-here")) {
                    val errorMsg = "Google Web Client ID not configured. Please set GOOGLE_WEB_CLIENT_ID in gradle.properties."
                    viewModel.handleGoogleSignInError(errorMsg)
                } else {
                    coroutineScope.launch {
                        val googleAuthHelper = GoogleAuthHelper(context)
                        googleAuthHelper.signIn(clientId).fold(
                            onSuccess = { idToken ->
                                viewModel.handleGoogleIdToken(idToken)
                            },
                            onFailure = { error ->
                                viewModel.handleGoogleSignInError(error.message ?: "Google Sign-In failed")
                            }
                        )
                    }
                }
            },
            onNavigateToMain = {
                navController.navigate(AppDestination.Home) {
                    popUpTo(AppDestination.Auth) { inclusive = true }
                }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    reelUploadManager: ReelUploadManager,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AppDestination.Home> {
        HomeScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this@composable,
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
            onNavigateToQuotePost = { postId ->
                navController.navigate(AppDestination.QuotePost(postId))
            },
            onNavigateToStoryViewer = { userId, allUserIds ->
                navController.navigate(AppDestination.StoryViewer(userId, allUserIds))
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

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.inboxGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AppDestination.Inbox> {
                InboxScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
                    },
                    onNavigateToChat = { chatId, userId, userName, avatar ->
                        navController.navigate(AppDestination.Chat(chatId, userId, userName, avatar))
                    },
                    onNavigateToSearch = {
                        navController.navigate(AppDestination.Search)
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
                    },
                    onNavigateToChatInfo = { chatId, userId ->
                        navController.navigate(AppDestination.ChatInfo(chatId, userId))
                    },
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
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
    composable<AppDestination.ChatInfo> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.ChatInfo>()
                com.synapse.social.studioasinc.feature.inbox.inbox.screens.ChatInfoScreen(
                    chatId = args.chatId,
                    userId = args.userId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId ->
                        navController.navigate(AppDestination.Profile(userId))
                    },
                    onNavigateToCreateGroup = {
                        navController.navigate(AppDestination.CreateGroup)
                    },
                    onNavigateToDisappearingMessages = { chatId ->
                        navController.navigate(AppDestination.DisappearingMessages(chatId))
                    },
                    onNavigateToSharedContent = { chatId, tab ->
                        navController.navigate(AppDestination.SharedContent(chatId, tab))
                    },
                    onNavigateToChatPrivacy = { chatId ->
                        navController.navigate(AppDestination.ChatPrivacy(chatId))
                    }
                )
            }
    composable<AppDestination.DisappearingMessages> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.DisappearingMessages>()
                com.synapse.social.studioasinc.feature.inbox.inbox.screens.DisappearingMessagesScreen(
                    chatId = args.chatId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
    composable<AppDestination.SharedContent> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.SharedContent>()
                com.synapse.social.studioasinc.feature.inbox.inbox.screens.SharedContentScreen(
                    chatId = args.chatId,
                    initialTab = args.initialTab,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
    composable<AppDestination.ChatPrivacy> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.ChatPrivacy>()
                com.synapse.social.studioasinc.feature.inbox.inbox.screens.ChatPrivacyScreen(
                    chatId = args.chatId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
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
                    onNavigateToChat = { targetUserId, userName, avatar ->
                        navController.navigate(AppDestination.Chat(chatId = "new", userId = targetUserId, participantName = userName, participantAvatar = avatar))
                    },
                    onNavigateToFollowers = {
                        navController.navigate(AppDestination.FollowList(userId, 0))
                    },
                    onNavigateToFollowing = {
                        navController.navigate(AppDestination.FollowList(userId, 1))
                    },
                    onNavigateToQuotePost = { postId ->
                        navController.navigate(AppDestination.QuotePost(postId))
                    },
                    onNavigateToUserProfile = { targetUid ->
                        navController.navigate(AppDestination.Profile(targetUid))
                    },
                    onNavigateToStoryCreator = {
                        context.startActivity(Intent(context, StoryCreatorActivity::class.java))
                    },
                    onNavigateToLockProfile = {
                        navController.navigate(AppDestination.LockProfile)
                    },
                    viewModel = viewModel
                )
            }
            composable<AppDestination.LockProfile> {
                val viewModel: LockProfileViewModel = hiltViewModel()
                LockProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AppDestination.EditProfile> {
                val viewModel: EditProfileViewModel = hiltViewModel()

                EditProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRegionSelection = { _ ->
                        navController.navigate(AppDestination.Location)
                    },
                    onNavigateToProfileMedia = {
                        navController.navigate(AppDestination.ProfileMedia)
                    },
                    onNavigateToPersonalInfo = {
                        navController.navigate(AppDestination.PersonalInformation)
                    },
                    onNavigateToPhotoHistory = { type ->
                        navController.navigate(AppDestination.PhotoHistory(type))
                    },
                    onNavigateToInterestsSkills = {
                        navController.navigate(AppDestination.InterestsSkills)
                    },
                    onNavigateToWorkEducation = {
                        navController.navigate(AppDestination.WorkEducation)
                    },
                    onNavigateToSocialLinks = {
                        navController.navigate(AppDestination.SocialLinks)
                    },
                    onNavigateToProfileAppearance = {
                        navController.navigate(AppDestination.ProfileAppearance)
                    },
                    onNavigateToFeaturedContent = {
                        navController.navigate(AppDestination.FeaturedContent)
                    },
                    onNavigateToProfessionalProfile = {
                        navController.navigate(AppDestination.ProfessionalProfile)
                    }
                )
            }
            composable<AppDestination.ProfessionalProfile> {
                com.synapse.social.studioasinc.feature.profile.editprofile.professional.ProfessionalProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToBusinessPlatform = {
                        navController.navigate(AppDestination.BusinessPlatform)
                    }
                )
            }
            composable<AppDestination.BusinessPlatform> {
                val viewModel: com.synapse.social.studioasinc.ui.settings.BusinessPlatformViewModel = hiltViewModel()
                val currentContext = androidx.compose.ui.platform.LocalContext.current
                com.synapse.social.studioasinc.ui.settings.BusinessPlatformScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNavigateToScheduledPosts = {
                        navController.navigate(AppDestination.ScheduledPosts)
                    },
                    onNavigateToContentCalendar = {
                        android.widget.Toast.makeText(currentContext, "Content Calendar coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToBrandPartnerships = {
                        android.widget.Toast.makeText(currentContext, "Brand Partnerships coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            composable<AppDestination.ScheduledPosts> {
                val viewModel: com.synapse.social.studioasinc.ui.settings.ScheduledPostsViewModel = hiltViewModel()
                com.synapse.social.studioasinc.ui.settings.ScheduledPostsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable<AppDestination.ProfileMedia> {
                ProfileMediaScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPhotoHistory = { type ->
                        navController.navigate(AppDestination.PhotoHistory(type))
                    }
                )
            }
            composable<AppDestination.PersonalInformation> {
                PersonalInformationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AppDestination.Location> {
                val viewModel: com.synapse.social.studioasinc.feature.profile.editprofile.location.LocationViewModel = hiltViewModel()

                val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                val selectedRegion = savedStateHandle?.get<String>("selected_region")

                LaunchedEffect(selectedRegion) {
                    selectedRegion?.let { region ->
                        viewModel.onEvent(com.synapse.social.studioasinc.feature.profile.editprofile.location.LocationEvent.RegionSelected(region))
                        savedStateHandle.remove<String>("selected_region")
                    }
                }

                com.synapse.social.studioasinc.feature.profile.editprofile.location.LocationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRegionSelection = {
                        navController.navigate(AppDestination.RegionSelection)
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
            composable<AppDestination.InterestsSkills> {
                InterestsSkillsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AppDestination.SocialLinks> {
                SocialLinksScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AppDestination.ProfileAppearance> {
                ProfileAppearanceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AppDestination.FeaturedContent> {
                FeaturedContentScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<AppDestination.WorkEducation> {
                WorkEducationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
     composable<AppDestination.FollowList> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.FollowList>()
                val userId = args.userId
                val currentUserId = AuthHelper.getCurrentUserId() ?: return@composable
                val targetUserId = if (userId == "me") currentUserId else userId
                val initialTab = args.initialTab
                FollowListScreen(
                    userId = targetUserId,
                    initialTab = initialTab,
                    onNavigateBack = { navController.popBackStack() },
                    onUserClick = { profileUserId ->
                        navController.navigate(AppDestination.Profile(profileUserId))
                    },
                    onMessageClick = { userId, userName, avatar ->
                        navController.navigate(AppDestination.Chat(chatId = "new", userId = userId, participantName = userName, participantAvatar = avatar))
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
                        // navController.navigate(AppDestination.ChatPrivacy)
                    },
                    onLogout = {
                        navController.navigate(AppDestination.Auth) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.postGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AppDestination.CreatePost> { backStackEntry ->
        val viewModel: CreatePostViewModel = hiltViewModel()
        val args = backStackEntry.toRoute<AppDestination.CreatePost>()
        val postId = args.postId
        val type = args.type
        val replyToPostId = args.replyToPostId

        LaunchedEffect(postId, replyToPostId, type) {
            viewModel.setCompositionType(type)
            if (postId != null) {
                viewModel.loadPostForEdit(postId)
            }
            if (replyToPostId != null) {
                viewModel.setReplyToPostId(replyToPostId)
            }
        }

        CreatePostScreen(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this@composable,
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
                onNavigateToQuotePost = { pid ->
                        navController.navigate(AppDestination.QuotePost(pid))
                },
                onNavigateToReplyToPost = { pid ->
                    navController.navigate(AppDestination.CreatePost(replyToPostId = pid))
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

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.hashtagGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AppDestination.HashtagFeed>(
        deepLinks = listOf(navDeepLink<AppDestination.HashtagFeed>(basePath = "synapse://hashtag"))
    ) { backStackEntry ->
        val args = backStackEntry.toRoute<AppDestination.HashtagFeed>()
        val viewModel: com.synapse.social.studioasinc.feature.hashtag.HashtagFeedViewModel = hiltViewModel()
        com.synapse.social.studioasinc.feature.hashtag.HashtagFeedScreen(
            tag = args.tag,
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onNavigateToPost = { postId, commentId ->
                navController.navigate(AppDestination.PostDetail(postId, commentId))
            },
            onNavigateToProfile = { userId ->
                navController.navigate(AppDestination.Profile(userId))
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.storyGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope
) {
    composable<AppDestination.StoryViewer> { backStackEntry ->
                val args = backStackEntry.toRoute<AppDestination.StoryViewer>()
                val userId = args.userId
                val userIds = args.userIds.ifEmpty { listOf(userId) }

                StoryPagerScreen(
                    userIds = userIds,
                    initialIndex = userIds.indexOf(userId).coerceAtLeast(0),
                    onClose = { navController.popBackStack() }
                )
            }
}
