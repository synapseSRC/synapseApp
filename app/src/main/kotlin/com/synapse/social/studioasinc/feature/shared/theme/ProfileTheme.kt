package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ProfileColors {
    val storyRingStart = StoryGradientStart
    val storyRingMiddle = StoryRingMiddle
    val storyRingEnd = StoryRingEnd

    val verifiedBadge: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
}

