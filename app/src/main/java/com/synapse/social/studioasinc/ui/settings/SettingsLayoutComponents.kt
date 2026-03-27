package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.sectionShape,
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}



@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)
    ) {
        Text(
            text = title,
            style = SettingsTypography.sectionHeader,
            color = SettingsColors.sectionTitle,
            modifier = Modifier.padding(start = Spacing.Small)
        )
        SettingsCard {
            content()
        }
    }
}



@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    items: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.sectionShape,
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            items()
        }
    }
}



@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = SettingsColors.divider,
        thickness = Sizes.BorderThin
    )
}
