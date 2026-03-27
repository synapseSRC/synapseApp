package com.synapse.social.studioasinc.feature.profile.profile

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.profile.profile.components.*
import com.synapse.social.studioasinc.feature.shared.components.MediaViewer
import com.synapse.social.studioasinc.feature.shared.components.post.PostOptionsBottomSheet

@Composable
internal fun ProfileMoreMenuSection(
    state: ProfileScreenState,
    viewModel: ProfileViewModel,
    effectiveIsOwnProfile: Boolean,
    profile: com.synapse.social.studioasinc.data.model.UserProfile?,
    context: Context,
    profileLinkLabel: String,
    postLinkLabel: String,
    onNavigateToSettings: () -> Unit
) {
    if (state.showMoreMenu) {
        ProfileMoreMenuBottomSheet(
            isOwnProfile = effectiveIsOwnProfile,
            onDismiss = { viewModel.toggleMoreMenu() },
            onShareProfile = { viewModel.showShareSheet() },
            onViewAs = { viewModel.showViewAsSheet() },
            onLockProfile = {
                profile?.let { viewModel.lockProfile(!it.isPrivate) }
            },
            onQrCode = { viewModel.showQrCode() },
            onSettings = onNavigateToSettings,
            onBlockUser = {
                profile?.let { viewModel.blockUser(it.id) }
            },
            onReportUser = { viewModel.showReportDialog() },
            onMuteUser = {
                profile?.let { viewModel.muteUser(it.id) }
            }
        )
    }
}

@Composable
internal fun ShareProfileSection(
    state: ProfileScreenState,
    viewModel: ProfileViewModel,
    profile: com.synapse.social.studioasinc.data.model.UserProfile?,
    context: Context,
    profileLinkLabel: String
) {
    if (state.showShareSheet) {
        ShareProfileBottomSheet(
            onDismiss = { viewModel.hideShareSheet() },
            onCopyLink = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText(profileLinkLabel, url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareToStory = {
                Toast.makeText(context, context.getString(R.string.toast_share_story_soon), Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareViaMessage = {
                Toast.makeText(context, context.getString(R.string.toast_share_message_soon), Toast.LENGTH_SHORT).show()
                viewModel.hideShareSheet()
            },
            onShareExternal = {
                val username = profile?.username ?: ""
                val url = "${BuildConfig.APP_DOMAIN}/profile/$username"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_profile_text, url))
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_profile)))
                viewModel.hideShareSheet()
            }
        )
    }
}

@Composable
internal fun ViewAsSection(
    state: ProfileScreenState,
    viewModel: ProfileViewModel,
    showUserSearchDialog: Boolean,
    onShowUserSearchDialog: (Boolean) -> Unit
) {
    if (state.showViewAsSheet) {
        ViewAsBottomSheet(
            onDismiss = { viewModel.hideViewAsSheet() },
            onViewAsPublic = {
                viewModel.setViewAsMode(ViewAsMode.PUBLIC)
                viewModel.hideViewAsSheet()
            },
            onViewAsFriends = {
                viewModel.setViewAsMode(ViewAsMode.FRIENDS)
                viewModel.hideViewAsSheet()
            },
            onViewAsSpecificUser = {
                onShowUserSearchDialog(true)
                viewModel.hideViewAsSheet()
            }
        )
    }
}

@Composable
internal fun UserSearchSection(
    showUserSearchDialog: Boolean,
    viewModel: ProfileViewModel,
    state: ProfileScreenState,
    onDismiss: () -> Unit
) {
    if (showUserSearchDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        UserSearchDialog(
            onDismiss = {
                onDismiss()
                viewModel.clearSearchResults()
            },
            onUserSelected = { user ->
                onDismiss()
                viewModel.clearSearchResults()
                viewModel.setViewAsMode(ViewAsMode.SPECIFIC_USER, user.username ?: context.getString(R.string.default_user_name))
            },
            onSearch = { query ->
                viewModel.searchUsers(query)
            },
            searchResults = state.searchResults,
            isSearching = state.isSearching
        )
    }
}

@Composable
internal fun QRCodeSection(
    state: ProfileScreenState,
    viewModel: ProfileViewModel,
    profile: com.synapse.social.studioasinc.data.model.UserProfile?
) {
    if (state.showQrCode) {
        QRCodeDialog(
            profileUrl = "${BuildConfig.APP_DOMAIN}/profile/${profile?.username ?: ""}",
            username = profile?.username ?: "",
            onDismiss = { viewModel.hideQrCode() }
        )
    }
}

@Composable
internal fun ReportSection(
    state: ProfileScreenState,
    viewModel: ProfileViewModel
) {
    if (state.showReportDialog) {
        val profile = (state.profileState as? ProfileUiState.Success)?.profile
        profile?.let {
            ReportUserDialog(
                username = it.username,
                onDismiss = { viewModel.hideReportDialog() },
                onReport = { reason -> viewModel.reportUser(it.id, reason) }
            )
        }
    }
}

@Composable
internal fun MediaViewerSection(
    showMediaViewer: Boolean,
    selectedMediaUrls: List<String>,
    initialMediaPage: Int,
    onDismiss: () -> Unit
) {
    if (showMediaViewer && selectedMediaUrls.isNotEmpty()) {
        MediaViewer(
            mediaUrls = selectedMediaUrls,
            initialPage = initialMediaPage,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun PostOptionsSection(
    showPostOptions: Boolean,
    selectedPost: Post?,
    currentUserId: String,
    state: ProfileScreenState,
    viewModel: ProfileViewModel,
    context: Context,
    postLinkLabel: String,
    onNavigateToEditPost: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (showPostOptions && selectedPost != null) {
        val post = selectedPost
        PostOptionsBottomSheet(
            post = post,
            isOwner = (post.authorUid == currentUserId) && (state.viewAsMode == null),
            commentsDisabled = post.postDisableReplies == "true",
            onDismiss = onDismiss,
            onEdit = {
                onDismiss()
                onNavigateToEditPost(post.id)
            },
            onDelete = {
                viewModel.deletePost(post.id)
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_post_text, "${BuildConfig.APP_DOMAIN}/post/${post.id}"))
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.title_share_post)))
            },
            onCopyLink = {
                val url = "${BuildConfig.APP_DOMAIN}/post/${post.id}"
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText(postLinkLabel, url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            },
            onBookmark = {
                viewModel.toggleSave(post.id)
            },
            onToggleComments = {
                Toast.makeText(context, context.getString(R.string.toast_toggle_comments_impl), Toast.LENGTH_SHORT).show()
            },
            onReport = {
                viewModel.reportPost(post.id, context.getString(R.string.toast_reported_from_profile))
                Toast.makeText(context, context.getString(R.string.toast_report_submitted), Toast.LENGTH_SHORT).show()
            },
            onBlock = {
                viewModel.blockUser(post.authorUid)
            },
            onRevokeVote = {
                Toast.makeText(context, context.getString(R.string.toast_revoke_vote_impl), Toast.LENGTH_SHORT).show()
            }
        )
    }
}
