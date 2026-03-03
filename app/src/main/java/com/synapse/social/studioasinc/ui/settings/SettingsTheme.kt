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
        get() = MaterialTheme.colorScheme.surfaceContainer



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


    val cardShape: Shape = RoundedCornerShape(28.dp)



    val sectionShape: Shape = RoundedCornerShape(24.dp)



    val itemShape: Shape = RoundedCornerShape(16.dp)



    val inputShape: Shape = RoundedCornerShape(12.dp)



    val chipShape: Shape = RoundedCornerShape(8.dp)
}



object SettingsSpacing {


    val screenPadding: Dp = 16.dp



    val sectionSpacing: Dp = 12.dp



    val itemSpacing: Dp = 0.dp



    val itemPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)



    val itemHorizontalPadding: Dp = 16.dp



    val itemVerticalPadding: Dp = 12.dp



    val iconSize: Dp = 24.dp



    val avatarSize: Dp = 64.dp



    val profileHeaderPadding: Dp = 16.dp



    val iconTextSpacing: Dp = 16.dp



    val minTouchTarget: Dp = 48.dp
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
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        Middle -> RoundedCornerShape(0.dp)
        Bottom -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }
}
