package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        get() = MaterialTheme.colorScheme.onSurfaceVariant



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
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)



    val divider: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)



    val itemIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant
}



object SettingsShapes {


    val cardShape: Shape = RoundedCornerShape(Sizes.CornerMassive)



    val sectionShape: Shape = RoundedCornerShape(Sizes.CornerExtraLarge)



    val itemShape: Shape = RoundedCornerShape(Sizes.CornerLarge)



    val inputShape: Shape = RoundedCornerShape(Sizes.CornerDefault)



    val chipShape: Shape = RoundedCornerShape(Sizes.CornerMedium)
}



object SettingsSpacing {


    val screenPadding: Dp = Spacing.Medium



    val sectionSpacing: Dp = Spacing.SmallMedium



    val itemSpacing: Dp = 0.dp



    val itemPadding: PaddingValues = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)



    val itemHorizontalPadding: Dp = Spacing.Medium



    val itemVerticalPadding: Dp = Spacing.SmallMedium



    val iconSize: Dp = Spacing.Large



    val avatarSize: Dp = 64.dp



    val profileHeaderPadding: Dp = Spacing.Medium



    val iconTextSpacing: Dp = Spacing.Medium



    val minTouchTarget: Dp = Spacing.Huge
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



    fun getShape(): Shape = when (this) {
        Single -> SettingsShapes.itemShape
        Top -> RoundedCornerShape(
            topStart = Sizes.CornerLarge,
            topEnd = Sizes.CornerLarge,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        Middle -> RoundedCornerShape(0.dp)
        Bottom -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = Sizes.CornerLarge,
            bottomEnd = Sizes.CornerLarge
        )
    }
}
