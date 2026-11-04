package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

@Composable
internal fun ChatSearchResults(
    query: String,
    isSearching: Boolean,
    results: List<Message>,
    currentUserId: String,
    onResultClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    senderAvatarUrl: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        when {
            isSearching -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            query.isBlank() -> {
                SearchPlaceholder(
                    icon = Icons.Default.Search,
                    text = stringResource(R.string.search_messages_empty)
                )
            }
            results.isEmpty() -> {
                SearchPlaceholder(
                    icon = Icons.Default.SearchOff,
                    text = stringResource(R.string.no_search_results)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.space16),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    items(results, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            currentUserId = currentUserId,
                            isFromMe = message.isFromMe(currentUserId),
                            senderName = senderName,
                            senderAvatarUrl = senderAvatarUrl,
                            onClick = { onResultClick(message) },
                            onLongClick = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SearchPlaceholder(
    icon: ImageVector,
    text: String
) {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Sizes.IconExtraLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.space16))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
