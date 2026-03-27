package com.synapse.social.studioasinc.feature.profile.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.post.PostDetailActivity
import com.synapse.social.studioasinc.feature.profile.profile.animations.crossfadeContent
import com.synapse.social.studioasinc.feature.profile.profile.components.*
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.PostActionsFactory
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.EmptyState
import kotlinx.coroutines.delay

@Composable
internal fun ProfileContent(
    state: ProfileScreenState,
    profile: com.synapse.social.studioasinc.data.model.UserProfile,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollProgress: Float,
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToQuotePost: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToChat: (String, String?, String?) -> Unit,
    onNavigateToStoryCreator: () -> Unit,
    onCustomizeClick: () -> Unit = {},
    onOpenMediaViewer: (List<String>, Int) -> Unit,
    onShowPostOptions: (Post) -> Unit
) {

    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "contentAlpha"
    )

    val context = LocalContext.current

    var bioExpanded by remember { mutableStateOf(false) }

    val currentOnNavigateToUserProfile by rememberUpdatedState(onNavigateToUserProfile)
    val currentOnNavigateToQuotePost by rememberUpdatedState(onNavigateToQuotePost)
    val currentOnOpenMediaViewer by rememberUpdatedState(onOpenMediaViewer)
    val currentOnShowPostOptions by rememberUpdatedState(onShowPostOptions)

    val actions = remember(context, viewModel) {
        PostActionsFactory.create(
            viewModel = viewModel,
            onComment = { post ->
                val intent = Intent(context, PostDetailActivity::class.java).apply {
                    putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
                    putExtra(PostDetailActivity.EXTRA_AUTHOR_UID, post.authorUid)
                }
                context.startActivity(intent)
            },
            onShare = { post ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_text, "${BuildConfig.APP_DOMAIN}/post/${post.id}"))
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_post)))
            },
            onQuote = { post ->
                currentOnNavigateToQuotePost(post.id)
            },
            onUserClick = { userId -> currentOnNavigateToUserProfile(userId) },
            onOptionClick = { post -> currentOnShowPostOptions(post) },
            onMediaClick = { index -> }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha }
    ) {

        if (state.viewAsMode != null) {
            item {
                ViewAsBanner(
                    viewMode = state.viewAsMode,
                    specificUserName = state.viewAsUserName,
                    onExitViewAs = { viewModel.exitViewAs() }
                )
            }
        }

        item {
            ProfileHeader(
                avatar = profile.avatar,
                status = profile.status,
                coverImageUrl = profile.coverImageUrl,
                name = profile.name,
                username = profile.username,
                nickname = profile.nickname,
                bio = profile.bio,
                isVerified = profile.isVerified,
                hasStory = state.hasStory,
                postsCount = profile.postCount,
                followersCount = profile.followerCount,
                followingCount = profile.followingCount,
                isOwnProfile = state.isOwnProfile && state.viewAsMode == null,
                isFollowing = state.isFollowing,
                isFollowLoading = state.isFollowLoading,
                scrollOffset = scrollProgress,
                bioExpanded = bioExpanded,
                onToggleBio = { bioExpanded = !bioExpanded },
                onProfileImageClick = {
                     if (state.isOwnProfile) {
                         onNavigateToEditProfile()
                     } else if (!profile.avatar.isNullOrBlank()) {
                         onOpenMediaViewer(listOf(profile.avatar), 0)
                     }
                },
                onCoverPhotoClick = {
                     if (state.isOwnProfile) {
                         onNavigateToEditProfile()
                     } else if (!profile.coverImageUrl.isNullOrBlank()) {
                         onOpenMediaViewer(listOf(profile.coverImageUrl), 0)
                     }
                },
                onEditProfileClick = onNavigateToEditProfile,
                onFollowClick = {
                    if (state.isFollowing) {
                        viewModel.unfollowUser(profile.id)
                    } else {
                        viewModel.followUser(profile.id)
                    }
                },
                onMessageClick = { onNavigateToChat(profile.id, profile.name ?: profile.username, profile.avatar) },
                onAddStoryClick = onNavigateToStoryCreator,
                onMoreClick = { viewModel.toggleMoreMenu() },
                onStatsClick = { stat ->
                    when (stat) {
                        "followers" -> onNavigateToFollowers()
                        "following" -> onNavigateToFollowing()
                    }
                }
            )
        }
        item {
            val prof = (state.profileState as? ProfileUiState.Success)?.profile
            if (prof != null) {
                Column {

                            Spacer(modifier = Modifier.height(Spacing.Medium))
                            UserDetailsSection(
                                details = UserDetails(
                                    location = prof.location,
                                    joinedDate = formatJoinedDate(prof.joinedDate),
                                    relationshipStatus = prof.relationshipStatus,
                                    birthday = prof.birthday,
                                    work = prof.work,
                                    education = prof.education,
                                    website = prof.website,
                                    gender = prof.gender,
                                    pronouns = prof.pronouns,
                                    linkedAccounts = prof.linkedAccounts.map {
                                        LinkedAccount(
                                            platform = it.platform,
                                            username = it.username
                                        )
                                    },
                                    currentCity = prof.currentCity,
                                    hometown = prof.hometown,
                                    occupation = prof.occupation,
                                    workplace = prof.workplace,
                                    discordTag = prof.discordTag,
                                    githubProfile = prof.githubProfile,
                                    personalWebsite = prof.personalWebsite,
                                    publicEmail = prof.publicEmail
                                ),
                                isOwnProfile = state.isOwnProfile,
                                onCustomizeClick = onCustomizeClick,
                                onWebsiteClick = { url ->
                                     IntentUtils.openUrl(context, url)
                                 },
                                modifier = Modifier.padding(horizontal = Spacing.Medium)
                            )

                            Spacer(modifier = Modifier.height(Spacing.Medium))

                            FollowingSection(
                                users = state.followingList,
                                selectedFilter = FollowingFilter.ALL,
                                onFilterSelected = { },
                                onUserClick = { user -> onNavigateToUserProfile(user.id) },
                                onSeeAllClick = onNavigateToFollowing,
                                modifier = Modifier.padding(horizontal = Spacing.Medium)
                            )

                            Spacer(modifier = Modifier.height(Spacing.Medium))
                }
            }

        }

        item {
            Spacer(modifier = Modifier.height(Spacing.Small))
            ContentFilterBar(
                selectedFilter = state.contentFilter,
                onFilterSelected = { filter -> viewModel.switchContentFilter(filter) },
                modifier = Modifier.fillMaxWidth(),
                showLabels = true
            )
        }

        item {
            crossfadeContent(targetState = state.contentFilter) { filter ->
                when (filter) {
                    ProfileContentFilter.PHOTOS -> {
                        if (state.photos.isEmpty() && !state.isLoadingMore && !state.isRefreshing) {
                            EmptyState(
                                icon = Icons.Default.PhotoLibrary,
                                title = stringResource(R.string.empty_profile_photos_title),
                                message = stringResource(R.string.empty_profile_photos_msg)
                            )
                        } else {
                            val photos = remember(state.photos) {
                                state.photos.filterIsInstance<MediaItem>()
                            }
                            PhotoGrid(
                                items = photos,
                                onItemClick = { mediaItem ->
                                    val allUrls = photos.map { it.url }
                                    val index = photos.indexOf(mediaItem)
                                    onOpenMediaViewer(allUrls, if (index >= 0) index else 0)
                                },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                    ProfileContentFilter.POSTS -> {
                        val prof = (state.profileState as? ProfileUiState.Success)?.profile ?: return@crossfadeContent
                        Column {
                            if (state.posts.isEmpty() && !state.isLoadingMore && !state.isRefreshing) {
                                EmptyState(
                                    icon = Icons.AutoMirrored.Filled.Article,
                                    title = if (state.isOwnProfile) stringResource(R.string.empty_own_posts_title) else stringResource(R.string.empty_user_posts_title),
                                    message = if (state.isOwnProfile) stringResource(R.string.empty_own_posts_msg) else stringResource(R.string.empty_user_posts_msg)
                                )
                            }
                        }
                    }
                    ProfileContentFilter.REELS -> {
                        if (state.reels.isEmpty() && !state.isLoadingMore && !state.isRefreshing) {
                            EmptyState(
                                icon = Icons.Default.VideoLibrary,
                                title = stringResource(R.string.empty_profile_reels_title),
                                message = stringResource(R.string.empty_profile_reels_msg)
                            )
                        } else {
                            val reels = remember(state.reels) {
                                state.reels.filterIsInstance<MediaItem>()
                            }
                            ReelsGrid(
                                items = reels,
                                onItemClick = {
                                    Toast.makeText(context, context.getString(R.string.toast_reels_viewer_soon), Toast.LENGTH_SHORT).show()
                                },
                                isLoading = state.isLoadingMore
                            )
                        }
                    }
                    ProfileContentFilter.REPLIES -> {
                        if (state.replies.isEmpty() && !state.isLoadingMore && !state.isRefreshing) {
                            EmptyState(
                                icon = Icons.AutoMirrored.Filled.Article,
                                title = stringResource(com.synapse.social.studioasinc.R.string.no_replies),
                                message = stringResource(com.synapse.social.studioasinc.R.string.no_replies_msg)
                            )
                        }
                    }
                }
            }
        }

        if (state.contentFilter == ProfileContentFilter.POSTS && state.posts.isNotEmpty()) {
            val posts = state.posts.filterIsInstance<com.synapse.social.studioasinc.domain.model.Post>()
            itemsIndexed(posts, key = { index, it -> "${it.id}_${index}" }) { index, post ->

                val currentProfile = (state.profileState as? ProfileUiState.Success)?.profile

                val postActions = remember(actions, post) {
                    actions.copy(
                        onMediaClick = { idx ->
                            val urls = post.mediaItems?.mapNotNull { it.url } ?: listOfNotNull(post.postImage)
                            if (urls.isNotEmpty()) {
                                currentOnOpenMediaViewer(urls, idx)
                            }
                        },
                        onOptionClick = { p ->
                            currentOnShowPostOptions(p)
                        }
                    )
                }

                AnimatedPostCard(
                    post = post,
                    currentProfile = currentProfile,
                    actions = postActions
                )
            }
        }

        if (state.contentFilter == ProfileContentFilter.REPLIES && state.replies.isNotEmpty()) {
            val replies = state.replies.filterIsInstance<com.synapse.social.studioasinc.domain.model.CommentWithUser>()
            itemsIndexed(replies, key = { index, it -> "${it.id}_${index}" }) { index, comment ->
                val postCardState = remember(comment) {
                    com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper.toPostCardState(
                        comment = comment,
                        parentAuthorUsername = null, // Will be updated when parent author info is available in CommentWithUser
                        depth = 0,
                        showThreadLine = false,
                        isLastReply = false
                    )
                }

                PostCard(
                    state = postCardState,
                    onLikeClick = { viewModel.reactToPost(postCardState.post, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE) },
                    onCommentClick = { /* Navigate to detail */ },
                    onShareClick = { /* Share comment */ },
                    onRepostClick = { },
                    onBookmarkClick = { },
                    onUserClick = { comment.userId.let { onNavigateToUserProfile(it) } },
                    onPostClick = {
                        PostDetailActivity.start(context, comment.postId, comment.userId)
                    },
                    onMediaClick = { idx ->
                        val urls = listOfNotNull(comment.mediaUrl)
                        if (urls.isNotEmpty()) {
                            onOpenMediaViewer(urls, idx)
                        }
                    },
                    onOptionsClick = { /* Show options */ },
                    onPollVote = { /* No polls in comments */ },
                    onReactionSelected = { reaction -> /* Handle reaction */ },
                    onQuoteClick = { },
                    modifier = Modifier.padding(bottom = Spacing.Small)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
        }
    }
}

@Composable
internal fun AnimatedPostCard(
    post: com.synapse.social.studioasinc.domain.model.Post,
    currentProfile: com.synapse.social.studioasinc.data.model.UserProfile?,
    actions: PostActions
) {
    SharedPostItem(
        post = post,
        currentProfile = currentProfile,
        actions = actions
    )
}

internal fun formatJoinedDate(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
    return format.format(date)
}
