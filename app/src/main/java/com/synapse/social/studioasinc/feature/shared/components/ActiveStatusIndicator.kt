package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun ActiveStatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.IconSmall
) {
    if (isActive) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(StatusOnline)
                .border(Sizes.BorderDefault, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}
