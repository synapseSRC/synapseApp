package com.synapse.social.studioasinc.feature.hashtag

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.post.PostActions
import com.synapse.social.studioasinc.feature.shared.components.post.SharedPostItem
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagFeedScreen(
    tag: String,
    viewModel: HashtagFeedViewModel,
    onBack: () -> Unit,
    onNavigateToPost: (postId: String, commentId: String?) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tag) {
        viewModel.init(tag)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_hashtag_format, tag)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading && uiState.posts.isEmpty()) {
                ExpressiveLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && uiState.posts.isEmpty()) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.posts, key = { it.id }) { post ->
                        val actions = remember(viewModel, onNavigateToPost, onNavigateToProfile) {
                            PostActions(
                                onLike = { p -> viewModel.reactToPost(p, com.synapse.social.studioasinc.domain.model.ReactionType.LIKE) },
                                onComment = { p -> onNavigateToPost(p.rootPostId ?: p.inReplyToPostId ?: p.id, if (p.rootPostId != null || p.inReplyToPostId != null) p.id else null) },
                                onShare = { },
                                onRepost = { },
                                onQuote = { },
                                onBookmark = viewModel::bookmarkPost,
                                onOptionClick = { },
                                onPollVote = viewModel::votePoll,
                                onUserClick = { userId -> onNavigateToProfile(userId) },
                                onMediaClick = { _ -> onNavigateToPost(post.id, null) },
                                onReactionSelected = { p, r -> viewModel.reactToPost(p, r) }
                            )
                        }
                        SharedPostItem(post = post, actions = actions)
                    }
                }
            }
        }
    }
}
