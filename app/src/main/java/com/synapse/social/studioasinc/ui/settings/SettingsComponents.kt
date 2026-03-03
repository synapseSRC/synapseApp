package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.R










@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsToggleItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
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
fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsToggleItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.itemIcon
            )
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))


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
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsClickableItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
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
fun SettingsClickableItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsClickableItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.itemIcon
            )
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
                    Spacer(modifier = Modifier.height(4.dp))
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
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsNavigationItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
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
fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsNavigationItemContent(
        title = title,
        subtitle = subtitle,
        iconContent = {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(SettingsSpacing.iconSize),
                tint = SettingsColors.itemIcon
            )
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = SettingsTypography.itemSubtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))


            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
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
    @DrawableRes icon: Int? = null,
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
                        painter = painterResource(icon),
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.padding(start = if (icon != null) 40.dp else 0.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))


            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = Modifier.padding(start = if (icon != null) 40.dp else 0.dp)
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
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))


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

            Spacer(modifier = Modifier.height(4.dp))


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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = SettingsTypography.sectionHeader,
            color = SettingsColors.sectionTitle,
            modifier = Modifier.padding(start = 8.dp)
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
        thickness = 1.dp
    )
}







@Composable
fun ProfileHeaderCard(
    displayName: String,
    email: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val profileAvatarDescription = stringResource(R.string.settings_profile_avatar_description)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.profileHeaderPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier.size(SettingsSpacing.avatarSize)
            ) {
                if (avatarUrl != null && avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageLoader.buildImageRequest(LocalContext.current, avatarUrl),
                        contentDescription = "$profileAvatarDescription, $displayName",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_person),
                                contentDescription = "$profileAvatarDescription, $displayName",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }


                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {}
            }

            Spacer(modifier = Modifier.width(16.dp))


            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    style = SettingsTypography.profileName,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    style = SettingsTypography.profileEmail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun SettingsInfoItem(
    title: String,
    value: String,
    @DrawableRes icon: Int? = null,
    onCopy: (() -> Unit)? = null
) {
    val modifier = if (onCopy != null) {
        Modifier
            .clickable(onClick = onCopy)
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
    } else {
        Modifier
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
            }

            Column {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = SettingsTypography.itemSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (onCopy != null) {
             Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
