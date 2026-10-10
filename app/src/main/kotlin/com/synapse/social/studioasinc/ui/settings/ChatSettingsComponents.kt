package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ChatRadioItem(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single,
    modifier: Modifier = Modifier
) {
    val radioDescription = if (selected) "Selected, $title" else "Not selected, $title"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick
                )
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    contentDescription = radioDescription
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null, // parent handles touch/click
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
