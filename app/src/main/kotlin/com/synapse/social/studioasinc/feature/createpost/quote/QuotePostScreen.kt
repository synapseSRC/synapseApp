package com.synapse.social.studioasinc.feature.createpost.quote

import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.components.post.QuotedPostCard

import androidx.compose.ui.Alignment
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotePostScreen(
    viewModel: QuotePostViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPost: (postId: String, commentId: String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var quoteText by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            state.post?.let { post ->
                onNavigateToPost(post.id, null)
            }
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quote_post_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                    .padding(Spacing.space16)
                    .heightIn(min = 120.dp),
                maxLines = 10,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.space16))

            Text(
                text = stringResource(R.string.quoting),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(Spacing.space16)
            )

            val post = state.post
            if (state.isLoading && post == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.space32), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (post != null) {
                QuotedPostCard(
                    quotedPost = post,
                    modifier = Modifier.padding(horizontal = Spacing.space16)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space16))
        }
    }
}
