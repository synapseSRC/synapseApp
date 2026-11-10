package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.components.LocalGlassBackdrop
import com.synapse.social.studioasinc.feature.shared.components.liquidGlassSurface
import com.synapse.social.studioasinc.feature.shared.components.rememberLiquidGlassPaint
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

/**
 * The day / history chip that separates one stretch of the conversation from the next.
 *
 * It wears the material the header and the composer wear. It cannot sample the layer that holds the
 * conversation, because it is drawn inside it (see LocalGlassBackdrop), so it samples the
 * wallpaper-only layer - which is what is behind a chip anyway: a chip sits in the list's own flow,
 * with nothing between it and the wallpaper. Where no backdrop is provided - below Android 12, or a
 * screen that has no layer - the plain container it always had is drawn exactly as before.
 */
@Composable
fun SystemHistoryChip(message: Message) {
    val backdrop = LocalGlassBackdrop.current
    val paint = rememberLiquidGlassPaint()
    val chipShape = remember { RoundedCornerShape(12.dp) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space8),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .then(
                    if (backdrop != null) {
                        Modifier.liquidGlassSurface(
                            backdrop = backdrop,
                            shape = chipShape,
                            paint = paint
                        )
                    } else {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = chipShape
                        )
                    }
                )
                .padding(horizontal = Spacing.space12, vertical = Spacing.space4)
        )
    }
}
