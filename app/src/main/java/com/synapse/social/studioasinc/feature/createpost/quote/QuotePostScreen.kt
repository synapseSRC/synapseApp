package com.synapse.social.studioasinc.feature.createpost.quote

import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper

import androidx.compose.ui.Alignment
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePostScreen(
    viewModel: QuotePostViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var quoteText by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quote_post_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.quotePost(quoteText) },
                        enabled = quoteText.isNotBlank() && !state.isLoading
                    ) {
                        Text(stringResource(R.string.action_post))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = quoteText,
                onValueChange = { quoteText = it },
                placeholder = { Text(stringResource(R.string.quote_post_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium)
                    .heightIn(min = 120.dp),
                maxLines = 10
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.Medium))

            Text(
                text = "Quoting",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(Spacing.Medium)
            )

            if (state.isLoading && state.post == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.ExtraLarge), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.post != null) {
                Surface(
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    state.post?.let { PostUiMapper.toPostCardState(it) }?.let { PostCard(
 state = it,

                        onLikeClick = {},
                        onCommentClick = {},
                        onShareClick = {},
                        onRepostClick = {},
                        onBookmarkClick = {},
                        onUserClick = {},
                        onPostClick = {},
                        onMediaClick = { _ -> },
                        onOptionsClick = {},
                        onPollVote = {},
                        onQuoteClick = {},
                        modifier = Modifier.padding(Spacing.Small)
                    ) }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
        }
    }
}
