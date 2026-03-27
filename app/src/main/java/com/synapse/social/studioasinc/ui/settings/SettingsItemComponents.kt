package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsToggleItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = imageVector?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
            }
        },
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        position = position
    )
}

@Composable
private fun SettingsToggleItemContent(
    title: String,
    subtitle: String?,
    iconContent: (@Composable () -> Unit)?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    position: SettingsItemPosition
) {
    val toggleDescription = stringResource(R.string.settings_toggle_description)
    val fullDescription = "$title, $toggleDescription, ${if (checked) "enabled" else "disabled"}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .semantics(mergeDescendants = true) {
                    contentDescription = fullDescription
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (iconContent != null) {
                iconContent()
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            }


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

            Spacer(modifier = Modifier.width(Spacing.Medium))


            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}



@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsClickableItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = imageVector?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
            }
        },
        onClick = onClick,
        enabled = enabled,
        position = position
    )
}

@Composable
private fun SettingsClickableItemContent(
    title: String,
    subtitle: String?,
    iconContent: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    enabled: Boolean,
    position: SettingsItemPosition
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (iconContent != null) {
                iconContent()
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            }


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



@Composable
fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsNavigationItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = imageVector?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
            }
        },
        onClick = onClick,
        enabled = enabled,
        position = position
    )
}

@Composable
private fun SettingsNavigationItemContent(
    title: String,
    subtitle: String?,
    iconContent: (@Composable () -> Unit)?,
    onClick: () -> Unit,
    enabled: Boolean,
    position: SettingsItemPosition
) {
    val chevronDescription = stringResource(R.string.settings_chevron_description)
    val fullDescription = "$title, $chevronDescription"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = fullDescription
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (iconContent != null) {
                iconContent()
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            }


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

            Spacer(modifier = Modifier.width(Spacing.Medium))


            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.chevronIcon
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelectionItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    var expanded by remember { mutableStateOf(false) }
    val dropdownDescription = stringResource(R.string.settings_dropdown_description)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $dropdownDescription, $selectedOption"
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(SettingsSpacing.iconSize),
                        tint = SettingsColors.itemIcon
                    )
                    Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                }


                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.weight(1f)
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.padding(start = if (icon != null) Spacing.ButtonHeight else 0.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Small))


            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = Modifier.padding(start = if (icon != null) Spacing.ButtonHeight else 0.dp)
            ) {
                OutlinedTextField(
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = SettingsTypography.itemSubtitle
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelect(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun SettingsSliderItem(
    title: String,
    subtitle: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String = { it.toString() },
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    val sliderDescription = stringResource(R.string.settings_slider_description)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $sliderDescription, ${valueLabel(value)}"
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
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
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.SmallMedium))


            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))


            Text(
                text = valueLabel(value),
                style = SettingsTypography.itemSubtitle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}



@Composable
fun SettingsButtonItem(
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
        shape = SettingsShapes.itemShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isDestructive) SettingsColors.destructiveButton
                           else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isDestructive) SettingsColors.destructiveText
                          else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = title,
            style = SettingsTypography.buttonText
        )
    }
}



@Composable
fun SettingsHeaderItem(
    title: String
) {
    Text(
        text = title,
        style = SettingsTypography.sectionHeader,
        color = SettingsColors.sectionTitle,
        modifier = Modifier.padding(
            horizontal = SettingsSpacing.itemHorizontalPadding,
            vertical = SettingsSpacing.itemVerticalPadding
        )
    )
}
