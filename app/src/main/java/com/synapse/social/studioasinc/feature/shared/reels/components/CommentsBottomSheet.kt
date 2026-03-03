package com.synapse.social.studioasinc.feature.shared.reels.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.shared.reels.CommentsViewModel
import com.synapse.social.studioasinc.shared.domain.model.ReelComment
import com.synapse.social.studioasinc.ui.components.CircularAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    reelId: String,
    onDismiss: () -> Unit,
    viewModel: CommentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(reelId) {
        viewModel.loadComments(reelId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (uiState.isLoading && uiState.comments.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.error != null && uiState.comments.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Failed to load comments", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadComments(reelId) }) {
                            Text("Retry")
                        }
                    }
                } else if (uiState.comments.isEmpty()) {
                    Text(
                        text = "No comments yet. Be the first to comment!",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.comments) { comment ->
                            CommentItem(comment)
                        }
                    }
                }
            }


            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    TextButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(reelId, commentText)
                                commentText = ""
                            }
                        },
                        enabled = commentText.isNotBlank() && !uiState.isPosting
                    ) {
                        Text("Post")
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: ReelComment) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        CircularAvatar(
            imageUrl = comment.userAvatarUrl,
            contentDescription = null,
            size = 36.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = comment.userUsername ?: "Unknown",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = comment.createdAt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
