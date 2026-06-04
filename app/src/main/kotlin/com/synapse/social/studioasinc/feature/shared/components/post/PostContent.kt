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
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.components.LocalLinkMetadataUseCase
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewViewModel
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.domain.model.LinkPreview
import androidx.hilt.navigation.compose.hiltViewModel


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
    linkPreviews: List<LinkPreview>? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    getLinkMetadataUseCase: com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase? = null
) {
    Column(modifier = modifier) {
        if (!text.isNullOrBlank()) {
            var localExpanded by remember(text) { mutableStateOf(false) }
            var isTruncated by remember(text) { mutableStateOf(false) }
            val showFullText = isExpanded || localExpanded

            // Remember references to prevent redundant layouts on scroll
            var lastProcessedText by remember { mutableStateOf<String?>(null) }
            var lastProcessedShowFullText by remember { mutableStateOf<Boolean?>(null) }

            val context = LocalContext.current
            val colorOnSurface = MaterialTheme.colorScheme.onSurface

            Box {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextColor(colorOnSurface.toArgb())
                            textSize = 16f
                            movementMethod = android.text.method.LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        if (textView.tag != text) {
                            if (isExpanded) {
                                MarkdownRenderer.get(context).render(textView, text ?: "")
                            } else {
                                textView.text = text ?: ""
                            }
                            textView.tag = text
                        }
                        textView.setTextColor(colorOnSurface.toArgb())
                        textView.maxLines = if (showFullText) Int.MAX_VALUE else 10
                        textView.ellipsize = if (showFullText) null else TextUtils.TruncateAt.END

                        if (lastProcessedText != text || lastProcessedShowFullText != showFullText) {
                            lastProcessedText = text
                            lastProcessedShowFullText = showFullText
                            textView.post {
                                val layout = textView.layout ?: return@post
                                val fullLineCount = layout.lineCount
                                isTruncated = if (showFullText) {
                                    false
                                } else {
                                    fullLineCount >= 10 && layout.getEllipsisCount(fullLineCount - 1) > 0
                                }
                            }
                        }
                    }
                )

                if (isTruncated && !showFullText) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .clickable { localExpanded = true }
                    ) {
                        Text(
                            text = stringResource(R.string.see_more),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (showFullText && !isExpanded) {
                Text(
                    text = stringResource(R.string.show_less),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { localExpanded = false }
                )
            }
        }

        if (mediaUrls.isNotEmpty()) {
            MediaContent(
                mediaUrls = mediaUrls,
                isVideo = isVideo,
                postViewStyle = postViewStyle,
                onMediaClick = onMediaClick,
                modifier = Modifier
            )
        }

        // Show link previews
        val previewsToShow = remember(linkPreviews, text) {
            if (!linkPreviews.isNullOrEmpty()) {
                linkPreviews.take(3)
            } else {
                UrlUtils.extractUrls(text).take(3).map { url ->
                    LinkPreview(url = url)
                }
            }
        }

        if (previewsToShow.isNotEmpty()) {
            val resolvedUseCase = getLinkMetadataUseCase ?: LocalLinkMetadataUseCase.current
            if (resolvedUseCase != null) {
                previewsToShow.forEach { preview ->
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    LinkPreviewCard(
                        url = preview.url,
                        useCase = resolvedUseCase,
                        initialPreview = com.synapse.social.studioasinc.shared.domain.model.LinkPreview(
                            url = preview.url,
                            title = preview.title,
                            description = preview.description,
                            imageUrl = preview.imageUrl,
                            domain = preview.domain
                        ),
                        modifier = Modifier.padding(top = Spacing.ExtraSmall)
                    )
                }
            }
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
            Spacer(modifier = Modifier.height(Spacing.Small))
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
    val shape = RoundedCornerShape(MaterialTheme.shapes.medium.topStart)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onPostClick)
            .padding(Spacing.Medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(Spacing.Large)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(
                text = post.displayName ?: post.username ?: "Unknown",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (post.isVerified) {
                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                // Add verification icon if available, or just skip for now
            }
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(
                text = "@${post.username}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
 
        if (!post.postText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = post.postText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
 
        if (!post.mediaItems.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.Small))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = post.mediaItems?.firstOrNull()?.url ?: "",
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
