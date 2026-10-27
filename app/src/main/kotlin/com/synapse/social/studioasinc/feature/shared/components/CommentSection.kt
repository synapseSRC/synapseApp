package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

data class Comment(
    val id: String,
    val authorName: String,
    val text: String,
    val timestamp: String,
    val parentCommentId: String? = null,
    val replies: List<Comment> = emptyList()
)

@Composable
fun ExpandableCommentThread(comment: Comment, level: Int = 0) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        CommentItem(
            comment = comment,
            level = level,
            expanded = expanded,
            onExpandToggle = { expanded = !expanded }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            if (comment.replies.isNotEmpty()) {
                NestedCommentList(replies = comment.replies, level = level + 1)
            }
        }
    }
}

@Composable
fun NestedCommentList(replies: List<Comment>, level: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        replies.forEach { reply ->
            Spacer(modifier = Modifier.height(Spacing.space4))
            ExpandableCommentThread(comment = reply, level = level)
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    level: Int = 0,
    expanded: Boolean = false,
    onExpandToggle: () -> Unit = {}
) {
    val paddingStart = (level * Spacing.space16.value).dp.coerceAtMost(Spacing.space64)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.space4)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space4))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.space4))
                Row(
                    modifier = Modifier
                        .clickable { onExpandToggle() }
                        .padding(vertical = Spacing.space4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) stringResource(R.string.hide_replies) else stringResource(R.string.show_replies),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!expanded) {
                        Spacer(modifier = Modifier.width(Spacing.space8))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = comment.replies.size.toString(),
                                modifier = Modifier.padding(horizontal = Spacing.space4)
                            )
                        }
                    }
                }
            }
        }
    }
}