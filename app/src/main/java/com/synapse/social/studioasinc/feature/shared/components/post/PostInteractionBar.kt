package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.synapse.social.studioasinc.feature.shared.theme.InteractionIconDefault
import com.synapse.social.studioasinc.feature.shared.theme.InteractionLikeActive
import com.synapse.social.studioasinc.feature.shared.theme.InteractionRepostActive
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostInteractionBar(
    isLiked: Boolean,
    likeCount: Int,
    commentCount: Int,
    repostCount: Int = 0,
    viewsCount: Int = 0,
    isBookmarked: Boolean,
    isReshared: Boolean = false,
    hideLikeCount: Boolean = false,
    isComment: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onRepostClick: () -> Unit,
    onQuoteClick: () -> Unit = {},
    onBookmarkClick: () -> Unit,
    onReactionLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showRepostMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        val iconColor = InteractionIconDefault
        val likeActiveColor = InteractionLikeActive
        val repostActiveColor = InteractionRepostActive
        val blueAlpha = 0.1f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Comment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onCommentClick)
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = stringResource(
                        R.string.comment_on_post_with_count,
                        commentCount
                    ),
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                if (commentCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(commentCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor
                    )
                }
            }

            // Repost
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showRepostMenu = true }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = "Repost",
                        tint = if (isReshared) repostActiveColor else iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    if (repostCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCount(repostCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isReshared) repostActiveColor else iconColor,
                            fontWeight = if (isReshared) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                androidx.compose.material3.DropdownMenu(
                    expanded = showRepostMenu,
                    onDismissRequest = { showRepostMenu = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Reshare") },
                        onClick = {
                            onRepostClick()
                            showRepostMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Repeat, contentDescription = null, tint = repostActiveColor)
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Quote") },
                        onClick = {
                            onQuoteClick()
                            showRepostMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Like
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .combinedClickable(
                        onClick = onLikeClick,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReactionLongPress?.invoke()
                        }
                    )
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isLiked) R.string.like_post_liked else R.string.like_post_with_count,
                        likeCount
                    ),
                    tint = if (isLiked) likeActiveColor else iconColor,
                    modifier = Modifier.size(18.dp)
                )
                if (likeCount > 0 && !hideLikeCount) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLiked) likeActiveColor else iconColor,
                        fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Views
            if (!isComment) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = "Views",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    if (viewsCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCount(viewsCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor
                        )
                    }
                }
            }

            // Bookmark
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(
                        if (isBookmarked) R.string.unsave_post else R.string.save_post
                    ),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Share
            IconButton(
                onClick = onShareClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share_post),
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1000000 -> "${String.format("%.1f", count / 1000.0)}K"
        else -> "${String.format("%.1f", count / 1000000.0)}M"
    }
}
