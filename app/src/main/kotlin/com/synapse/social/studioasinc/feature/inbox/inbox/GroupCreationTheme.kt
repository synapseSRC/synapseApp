package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme

/** Rebuild the group palette from Android dynamic colors while preserving the parent light/dark mode. */
@Composable
internal fun GroupCreationTheme(content: @Composable () -> Unit) {
    val parentIsDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    SynapseTheme(
        darkTheme = parentIsDark,
        dynamicColor = true,
        content = content
    )
}

internal object GroupCreationDimensions {
    val qrCodeSize = 160.dp
}
