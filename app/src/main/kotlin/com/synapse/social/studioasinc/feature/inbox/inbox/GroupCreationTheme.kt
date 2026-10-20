package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTypography

private val GroupPrimaryLight = Color(0xFF6750A4)
private val GroupOnPrimaryLight = Color(0xFFFFFFFF)
private val GroupPrimaryContainerLight = Color(0xFFEADDFF)
private val GroupOnPrimaryContainerLight = Color(0xFF21005D)
private val GroupSecondaryContainerLight = Color(0xFFE8DEF8)
private val GroupOnSecondaryContainerLight = Color(0xFF1D192B)
private val GroupTertiaryContainerLight = Color(0xFFFFD8E4)
private val GroupOnTertiaryContainerLight = Color(0xFF31111D)
private val GroupPrimaryDark = Color(0xFFD0BCFF)
private val GroupOnPrimaryDark = Color(0xFF381E72)
private val GroupPrimaryContainerDark = Color(0xFF4F378B)
private val GroupOnPrimaryContainerDark = Color(0xFFEADDFF)
private val GroupSecondaryContainerDark = Color(0xFF4A4458)
private val GroupOnSecondaryContainerDark = Color(0xFFE8DEF8)
private val GroupTertiaryContainerDark = Color(0xFF633B48)
private val GroupOnTertiaryContainerDark = Color(0xFFFFD8E4)

/** Keeps Android dynamic surfaces while applying the violet accent and tonal cards from the reference. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GroupCreationTheme(content: @Composable () -> Unit) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    SynapseTheme(darkTheme = isDark, dynamicColor = true) {
        val base = MaterialTheme.colorScheme
        val groupScheme = if (isDark) {
            base.copy(
                primary = GroupPrimaryDark,
                onPrimary = GroupOnPrimaryDark,
                primaryContainer = GroupPrimaryContainerDark,
                onPrimaryContainer = GroupOnPrimaryContainerDark,
                secondaryContainer = GroupSecondaryContainerDark,
                onSecondaryContainer = GroupOnSecondaryContainerDark,
                tertiaryContainer = GroupTertiaryContainerDark,
                onTertiaryContainer = GroupOnTertiaryContainerDark
            )
        } else {
            base.copy(
                primary = GroupPrimaryLight,
                onPrimary = GroupOnPrimaryLight,
                primaryContainer = GroupPrimaryContainerLight,
                onPrimaryContainer = GroupOnPrimaryContainerLight,
                secondaryContainer = GroupSecondaryContainerLight,
                onSecondaryContainer = GroupOnSecondaryContainerLight,
                tertiaryContainer = GroupTertiaryContainerLight,
                onTertiaryContainer = GroupOnTertiaryContainerLight
            )
        }
        MaterialExpressiveTheme(
            colorScheme = groupScheme,
            motionScheme = MotionScheme.expressive(),
            typography = SynapseTypography,
            content = content
        )
    }
}

internal object GroupCreationDimensions {
    val qrCodeSize = 160.dp
}
