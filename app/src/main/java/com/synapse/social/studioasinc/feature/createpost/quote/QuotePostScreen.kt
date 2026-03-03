package com.synapse.social.studioasinc.feature.createpost.quote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.shared.components.post.PostCard
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePostScreen(
    quotedPost: Post,
    onNavigateBack: () -> Unit,
    onQuotePost: (String) -> Unit
) {
    var quoteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote Post") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onQuotePost(quoteText) },
                        enabled = quoteText.isNotBlank()
                    ) {
                        Text("Post")
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
                placeholder = { Text("Add your thoughts...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 120.dp),
                maxLines = 10
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                text = "Quoting",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(16.dp)
            )

            Surface(
                modifier = Modifier.padding(horizontal = 16.dp),
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                PostCard(
                    state = PostUiMapper.toPostCardState(quotedPost),
                    onLikeClick = {},
                    onCommentClick = {},
                    onShareClick = {},
                    onRepostClick = {},
                    onBookmarkClick = {},
                    onUserClick = {},
                    onPostClick = {},
                    onMediaClick = {},
                    onOptionsClick = {},
                    onPollVote = {},
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
