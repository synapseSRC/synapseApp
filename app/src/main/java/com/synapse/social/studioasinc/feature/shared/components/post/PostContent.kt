package com.synapse.social.studioasinc.feature.shared.components.post
 
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.Post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import com.synapse.social.studioasinc.styling.MarkdownRenderer
import android.text.TextUtils
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

import com.synapse.social.studioasinc.ui.settings.PostViewStyle

@Composable
fun PostContent(
    text: String?,
    mediaUrls: List<String>,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE,
    isVideo: Boolean,
    pollQuestion: String?,
    pollOptions: List<PollOption>? = null,
    userPollVote: Int? = null,
    onMediaClick: (Int) -> Unit = {},
    onPollVote: (String) -> Unit,
    quotedPost: Post? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!text.isNullOrBlank()) {
            var localExpanded by remember { mutableStateOf(false) }
            var isTruncated by remember { mutableStateOf(false) }
            val showFullText = isExpanded || localExpanded

            val context = LocalContext.current
            val colorOnSurface = MaterialTheme.colorScheme.onSurface

            Column {
                AndroidView(
                    modifier = Modifier
                        .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(colorOnSurface.toArgb())
                            textSize = 16f
                            movementMethod = android.text.method.LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        MarkdownRenderer.get(context).render(textView, text)
                        textView.setTextColor(colorOnSurface.toArgb())
                        textView.maxLines = if (showFullText) Int.MAX_VALUE else 10
                        textView.ellipsize = if (showFullText) null else TextUtils.TruncateAt.END
                        
                        // Check if text is truncated
                        textView.post {
                            val layout = textView.layout
                            if (layout != null) {
                                val lineCount = layout.lineCount
                                if (lineCount > 0) {
                                    val ellipsisCount = layout.getEllipsisCount(lineCount - 1)
                                    isTruncated = ellipsisCount > 0 || lineCount >= 10
                                }
                            }
                        }
                    }
                )
                
                if (isTruncated && !showFullText) {
                    Text(
                        text = stringResource(R.string.see_more),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = Spacing.SmallMedium)
                            .clickable { localExpanded = true }
                    )
                } else if (showFullText && !isExpanded) {
                    Text(
                        text = stringResource(R.string.show_less),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = Spacing.SmallMedium)
                            .clickable { localExpanded = false }
                    )
                }
            }
        }

        if (mediaUrls.isNotEmpty()) {
            MediaContent(
                mediaUrls = mediaUrls,
                isVideo = isVideo,
                postViewStyle = postViewStyle,
                onMediaClick = onMediaClick,
                modifier = Modifier.padding(horizontal = 0.dp)
            )
        }

        if (pollQuestion != null && pollOptions != null) {
            PollContent(
                question = pollQuestion,
                options = pollOptions,
                totalVotes = pollOptions.sumOf { it.voteCount },
                onVote = onPollVote
            )
        }
 
        if (quotedPost != null) {
            Spacer(modifier = Modifier.height(8.dp))
            QuotedPostCard(
                post = quotedPost,
                onPostClick = { /* Handled by parent or specific click */ }
            )
        }
    }
}
 
@Composable
fun QuotedPostCard(
    post: Post,
    onPostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onPostClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = post.displayName ?: post.username ?: "Unknown",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (post.isVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                // Add verification icon if available, or just skip for now
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "@${post.username}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
 
        if (!post.postText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.postText!!,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
 
        if (!post.mediaItems.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                AsyncImage(
                    model = post.mediaItems!!.first().url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
