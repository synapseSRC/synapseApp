package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun QuotedPostCard(
    quotedPost: Post,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(Sizes.BorderThin, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(Spacing.SmallMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text(
                    text = quotedPost.username ?: "Unknown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "@${quotedPost.username ?: "unknown"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            quotedPost.postText?.let { text ->
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3
                )
            }
            
            quotedPost.mediaItems?.firstOrNull()?.let {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = "📷 Media attached",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
