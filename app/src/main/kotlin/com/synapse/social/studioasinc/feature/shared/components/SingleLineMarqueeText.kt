package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * A [Text] that is always exactly one line.
 *
 * Long content used to wrap onto a second line and grow whatever contained it. Instead of hiding
 * the rest behind an ellipsis, the text scrolls inside the width it was given, so a long display
 * name or a long "Last seen ..." label can no longer change the height of a header.
 *
 * BasicMarquee only animates while the content is wider than its container, so short text renders
 * as a plain single line.
 */
@Composable
fun SingleLineMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    Text(
        text = text,
        modifier = modifier.basicMarquee(),
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}
