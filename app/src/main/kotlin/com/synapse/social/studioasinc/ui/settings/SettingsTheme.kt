package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
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
        get() = MaterialTheme.colorScheme.primary



    val cardBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)



    val screenBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.background



    val cardBackgroundElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainerHigh



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
        get() = MaterialTheme.colorScheme.primary

    val iconContainerBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
}



object SettingsShapes {


    val cardShape: Shape = RoundedCornerShape(24.dp)



    val sectionShape: Shape = RoundedCornerShape(24.dp)



    val itemShape: Shape = RoundedCornerShape(24.dp)

    val iconBadgeShape: Shape = RoundedCornerShape(14.dp)



    val inputShape: Shape = RoundedCornerShape(Sizes.CornerDefault)



    val chipShape: Shape = RoundedCornerShape(Sizes.CornerMedium)
}



object SettingsSpacing {


    val screenPadding: Dp = Spacing.Medium



    val sectionSpacing: Dp = Spacing.SmallMedium



    val itemSpacing: Dp = 3.dp

    val itemGap: Dp = 3.dp



    val itemPadding: PaddingValues = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)



    val itemHorizontalPadding: Dp = Spacing.Medium



    val itemVerticalPadding: Dp = Spacing.SmallMedium



    val iconSize: Dp = Spacing.Large



    val avatarSize: Dp = Sizes.AvatarSemiLarge



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



    fun getShape(): Shape = SettingsShapes.itemShape
}
