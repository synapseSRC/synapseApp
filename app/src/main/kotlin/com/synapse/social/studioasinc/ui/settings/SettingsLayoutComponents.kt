package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

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
        modifier = modifier.fillMaxWidth()
    ) {
        // The header carries its own vertical padding, matching the hub's section labels.
        SettingsHeaderItem(title = title)
        // Contained-list gap: items touch across a 3dp seam instead of sitting in a card.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
        ) {
            content()
        }
    }
}



@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    items: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
    ) {
        items()
    }
}



@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = SettingsColors.divider,
        thickness = Sizes.BorderThin
    )
}
