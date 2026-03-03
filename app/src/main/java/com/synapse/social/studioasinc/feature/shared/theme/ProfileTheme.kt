package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



object ProfileDimensions {

    val spacing0 = 0.dp
    val spacing2 = 2.dp
    val spacing4 = 4.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing20 = 20.dp
    val spacing24 = 24.dp
    val spacing32 = 32.dp
    val spacing48 = 48.dp


    val profileImageSmall = 40.dp
    val profileImageMedium = 56.dp
    val profileImageLarge = 88.dp
    val profileImageXLarge = 110.dp
    val profileImageSize = 96.dp


    val coverPhotoHeight = 180.dp
    val coverPhotoHeightExpanded = 220.dp


    val storyRingWidth = 3.dp
    val storyRingPadding = 3.dp


    val iconSizeSmall = 16.dp
    val iconSizeMedium = 20.dp
    val iconSize = 24.dp
    val iconSizeLarge = 32.dp


    val chipHeight = 32.dp
    val buttonHeight = 44.dp
    val buttonCornerRadius = 12.dp


    val photoGridSpacing = 2.dp
    val photoGridCornerRadius = 4.dp


    val cardCornerRadius = 16.dp
    val cardElevation = 2.dp


    val parallaxFactor = 0.5f
}



object ProfileAnimations {
    const val durationShort = 150
    const val durationMedium = 300
    const val durationLong = 500
    const val durationXLong = 800

    const val staggerDelay = 50
    const val countAnimationDuration = 600
    const val shimmerDuration = 1000
}



object ProfileColors {

    val storyRingStart = Color(0xFFE040FB)
    val storyRingMiddle = Color(0xFF7C4DFF)
    val storyRingEnd = Color(0xFF00BCD4)


    val coverOverlayStart = Color.Transparent
    val coverOverlayMiddle = Color.Black.copy(alpha = 0.1f)
    val coverOverlayEnd = Color.Black.copy(alpha = 0.4f)


    val verifiedBadge = Color(0xFF1976D2)


    val shimmerBase = Color(0xFFE0E0E0)
    val shimmerHighlight = Color(0xFFF5F5F5)
}



object ProfileBrushes {


    @Composable
    @ReadOnlyComposable
    fun storyRingGradient(): List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary
    )



    fun coverOverlayGradient(): Brush = Brush.verticalGradient(
        colors = listOf(
            ProfileColors.coverOverlayStart,
            ProfileColors.coverOverlayMiddle,
            ProfileColors.coverOverlayEnd
        )
    )



    fun shimmerGradient(offset: Float): Brush = Brush.linearGradient(
        colors = listOf(
            ProfileColors.shimmerBase.copy(alpha = 0.6f),
            ProfileColors.shimmerHighlight.copy(alpha = 0.2f),
            ProfileColors.shimmerBase.copy(alpha = 0.6f)
        ),
        start = androidx.compose.ui.geometry.Offset(offset - 200f, offset - 200f),
        end = androidx.compose.ui.geometry.Offset(offset, offset)
    )
}



val ProfileTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
)



fun Int.toFormattedCount(): String {
    return when {
        this >= 1_000_000_000 -> String.format("%.1fB", this / 1_000_000_000.0)
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 10_000 -> String.format("%.1fK", this / 1_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }.replace(".0", "")
}
