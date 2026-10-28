package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

object SettingsColors {
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

    val chevronIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val divider: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    /** Icon tint of a contained-list leading badge. */
    val itemIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onPrimaryContainer

    /** Container of a contained-list leading badge. */
    val iconContainerBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer

    val itemContent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurface

    val supportingText: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val disabledContentAlpha: Float = 0.38f
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

    /** Shape of an ungrouped contained-list item; matches the 28dp outer stack corner. */
    val itemShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = RoundedCornerShape(Sizes.CornerMassive)

    /** Contained-list leading badges are circular. */
    val iconBadgeShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = CircleShape

    val inputShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium

    val chipShape: Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.small
}

object SettingsSpacing {
    val screenPadding: Dp = Spacing.space16

    val sectionSpacing: Dp = Spacing.space12

    /** Contained-list treatment: 3dp gaps between stacked items, 28/8dp stacked corners. */
    val itemSpacing: Dp = 3.dp

    val itemPadding: PaddingValues = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space12)

    val itemHorizontalPadding: Dp = Spacing.space16

    val itemVerticalPadding: Dp = Spacing.space12

    val profileHeaderPadding: Dp = Spacing.space16

    val iconTextSpacing: Dp = Spacing.space16

    /** Content padding of a contained-list row; height comes from [rowMinHeight]. */
    val rowHorizontalPadding: Dp = Spacing.space16

    val rowVerticalPadding: Dp = Spacing.space4

    /** M3 two-line interactive container height used by the contained-list rows. */
    val rowMinHeight: Dp = Sizes.HeightAvatarLarge

    val iconBadgeSize: Dp = Sizes.IconMassive

    val rowIconSize: Dp = Sizes.IconLarge
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
        get() = MaterialTheme.typography.bodyLarge

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

    /** 28dp on the outer edge of the stack, 8dp where two items meet. */
    @Composable
    fun getShape(): Shape = when (this) {
        Single -> RoundedCornerShape(Sizes.CornerMassive)
        Top -> RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive,
            bottomStart = Sizes.CornerMedium,
            bottomEnd = Sizes.CornerMedium
        )
        Middle -> RoundedCornerShape(Sizes.CornerMedium)
        Bottom -> RoundedCornerShape(
            topStart = Sizes.CornerMedium,
            topEnd = Sizes.CornerMedium,
            bottomStart = Sizes.CornerMassive,
            bottomEnd = Sizes.CornerMassive
        )
    }
}
