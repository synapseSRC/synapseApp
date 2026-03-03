package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import com.synapse.social.studioasinc.styling.MarkdownRenderer
import android.text.TextUtils
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType

@Composable
fun PostContent(
    text: String?,
    mediaItems: List<MediaItem>?,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        text?.let {
            val context = LocalContext.current
            val colorOnSurface = MaterialTheme.colorScheme.onSurface

            AndroidView(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(colorOnSurface.toArgb())
                        textSize = 14f
                        movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    }
                },
                update = { textView ->
                    MarkdownRenderer.get(context).render(textView, it)
                    textView.setTextColor(colorOnSurface.toArgb())
                    textView.maxLines = 10
                    textView.ellipsize = TextUtils.TruncateAt.END
                }
            )
        }

        mediaItems?.firstOrNull()?.let { media ->
            AsyncImage(
                model = media.url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentScale = ContentScale.Crop
            )

            if (mediaItems.size > 1) {
                Text(
                    text = "+${mediaItems.size - 1} more",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun PostContentPreview() {
    MaterialTheme {
        PostContent(
            text = "This is a sample post text with some content",
            mediaItems = null,
            onMediaClick = {}
        )
    }
}
