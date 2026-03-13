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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSection(
    comments: List<Comment>,
    onAddComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCommentText by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.SmallMedium)
        ) {
            items(comments) { comment ->
                ExpandableCommentThread(comment)
                Spacer(modifier = Modifier.height(Spacing.Small))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.comment_write)) }
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        onAddComment(newCommentText)
                        newCommentText = ""
                    }
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.cd_send))
            }
        }
    }
}

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
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
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
    val paddingStart = (level * 16).dp.coerceAtMost(64.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.ExtraSmall)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Row(
                    modifier = Modifier
                        .clickable { onExpandToggle() }
                        .padding(vertical = Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) stringResource(R.string.hide_replies) else stringResource(R.string.show_replies),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!expanded) {
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = comment.replies.size.toString(),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}