package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

object SettingsColors {

    val categoryIconTint: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val categoryBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer

    val sectionTitle: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val cardBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerLow

    val screenBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background

    val cardBackgroundElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val destructiveButton: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.errorContainer

    val destructiveText: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onErrorContainer

    val toggleActive: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val toggleTrack: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceVariant

    val chevronIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    val divider: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    val itemIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val iconContainerBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh
}

object SettingsShapes {

    val cardShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.large

    val sectionShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.large

    val itemShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.large

    val iconBadgeShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium

    val inputShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium

    val chipShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.small

    private val scallopedPolygon = RoundedPolygon(
        numVertices = 8,
        radius = 0.5f,
        centerX = 0.5f,
        centerY = 0.5f,
        rounding = CornerRounding(0.5f)
    )

    val scallopedShape: Shape = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = scallopedPolygon.toPath().asComposePath()
            val matrix = Matrix().apply {
                scale(size.width, size.height)
            }
            path.transform(matrix)
            return Outline.Generic(path)
        }
    }
}

object SettingsSpacing {

    val screenPadding: Dp = Spacing.space16

    val sectionSpacing: Dp = Spacing.space12

    val itemSpacing: Dp = Spacing.space4

    val itemPadding: PaddingValues = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space12)

    val itemHorizontalPadding: Dp = Spacing.space16

    val itemVerticalPadding: Dp = Spacing.space12

    val profileHeaderPadding: Dp = Spacing.space16

    val iconTextSpacing: Dp = Spacing.space16

}

object SettingsTypography {

    val screenTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineMedium

    val sectionHeader: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium

    val itemTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)

    val itemSubtitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium

    val profileName: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleLarge

    val profileEmail: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium

    val buttonText: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.labelLarge
}

enum class SettingsItemPosition {

    Single,

    Top,

    Middle,

    Bottom;

    @Composable
    fun getShape(): Shape = when (this) {
        Single -> SettingsShapes.itemShape
        Top -> RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive,
            bottomStart = Sizes.CornerSmall,
            bottomEnd = Sizes.CornerSmall
        )
        Middle -> RoundedCornerShape(Sizes.CornerSmall)
        Bottom -> RoundedCornerShape(
            topStart = Sizes.CornerSmall,
            topEnd = Sizes.CornerSmall,
            bottomStart = Sizes.CornerMassive,
            bottomEnd = Sizes.CornerMassive
        )
    }
}
