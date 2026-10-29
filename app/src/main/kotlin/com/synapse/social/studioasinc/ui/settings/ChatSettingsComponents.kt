package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.feature.shared.theme.AccessibilitySizes

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
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

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
        color = AppColors.cardBackground
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
                    horizontal = AppSpacing.itemHorizontalPadding,
                    vertical = AppSpacing.itemVerticalPadding
                )
                .heightIn(min = AccessibilitySizes.minimumTouchTarget),
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

            Spacer(modifier = Modifier.width(AppSpacing.iconTextSpacing))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = AppTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Spacing.space4))
                    Text(
                        text = subtitle,
                        style = AppTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}
