package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.layout.Column
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

import com.synapse.social.studioasinc.ui.settings.PostViewStyle

@Composable
fun PostContent(
    text: String?,
    mediaUrls: List<String>,
    postViewStyle: PostViewStyle = PostViewStyle.SWIPE,
    isVideo: Boolean,
    pollQuestion: String?,
    pollOptions: List<PollOption>?,
    onMediaClick: (Int) -> Unit,
    onPollVote: (String) -> Unit,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (!text.isNullOrBlank()) {
            var localExpanded by remember { mutableStateOf(false) }
            val showFullText = isExpanded || localExpanded

            val context = LocalContext.current
            val colorOnSurface = MaterialTheme.colorScheme.onSurface

            AndroidView(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    textView.setOnClickListener { localExpanded = !localExpanded }
                }
            )
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
    }
}
