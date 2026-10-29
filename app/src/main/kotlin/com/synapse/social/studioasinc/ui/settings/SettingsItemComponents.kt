package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Contained-list treatment shared with the profile more menu sheet: 72dp rows, circular
 * primaryContainer badges, 28/8dp stacked corners, 3dp gaps and a bodyLarge / bodyMedium pair.
 */
@Composable
fun SettingsIconBadge(
    imageVector: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(SettingsSpacing.iconBadgeSize),
        shape = SettingsShapes.iconBadgeShape,
        color = SettingsColors.iconContainerBackground,
        contentColor = SettingsColors.itemIcon
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(SettingsSpacing.rowIconSize)
            )
        }
    }
}

@Composable
fun SettingsNavigationChevron(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.ChevronRight
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(SettingsSpacing.rowIconSize),
        tint = SettingsColors.chevronIcon
    )
}

/** Flag that the row has something unread behind it; [description] carries that to TalkBack. */
@Composable
fun SettingsUnreadDot(
    description: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Sizes.StatusDotSmall)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = description }
    )
}

/**
 * Single source of truth for a settings row. Content is pinned to the vertical centre of the
 * 72dp container, the same as [com.synapse.social.studioasinc.feature.profile.profile.components.ProfileMoreMenuBottomSheet].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    position: SettingsItemPosition = SettingsItemPosition.Single,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val shape = position.getShape()
    val interactionSource: MutableInteractionSource? =
        if (onClick != null) remember { MutableInteractionSource() } else null
    val pressed = interactionSource?.collectIsPressedAsState()?.value == true
    val scale by animateFloatAsState(
        targetValue = if (pressed) SettingsAnimations.pressScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "settingsListItemPress"
    )

    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = SettingsSpacing.rowMinHeight)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }

    val contentPadding = PaddingValues(
        start = SettingsSpacing.rowHorizontalPadding,
        end = SettingsSpacing.rowHorizontalPadding,
        top = SettingsSpacing.rowVerticalPadding,
        bottom = SettingsSpacing.rowVerticalPadding
    )

    val headline: @Composable () -> Unit = {
        Text(
            text = title,
            style = SettingsTypography.itemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    val supporting: (@Composable () -> Unit)? = subtitle?.let { text ->
        {
            Text(
                text = text,
                style = SettingsTypography.itemSubtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    val colors = ListItemDefaults.colors(
        containerColor = SettingsColors.cardBackground,
        contentColor = SettingsColors.itemContent,
        leadingContentColor = SettingsColors.itemIcon,
        trailingContentColor = SettingsColors.chevronIcon,
        supportingContentColor = SettingsColors.supportingText,
        disabledContentColor = SettingsColors.itemContent.copy(alpha = SettingsColors.disabledContentAlpha),
        disabledSupportingContentColor = SettingsColors.supportingText.copy(alpha = SettingsColors.disabledContentAlpha)
    )

    // Stacked corners must not morph on press, so every state reuses the segment shape.
    val shapes = ListItemDefaults.shapes(
        shape = shape,
        pressedShape = shape,
        focusedShape = shape,
        hoveredShape = shape
    )

    if (onClick != null) {
        ListItem(
            onClick = onClick,
            modifier = rowModifier,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            supportingContent = supporting,
            verticalAlignment = Alignment.CenterVertically,
            shapes = shapes,
            colors = colors,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = headline
        )
    } else {
        ListItem(
            modifier = rowModifier,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            supportingContent = supporting,
            verticalAlignment = Alignment.CenterVertically,
            shapes = shapes,
            colors = colors,
            contentPadding = contentPadding,
            content = headline
        )
    }
}

/** Colour a row's headline / supporting text carries when the row is disabled. */
private fun settingsContentAlpha(enabled: Boolean, content: Color): Color =
    if (enabled) content else content.copy(alpha = SettingsColors.disabledContentAlpha)

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
    SettingsListItem(
        title = title,
        subtitle = subtitle,
        position = position,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        leadingContent = imageVector?.let {
            {
                SettingsIconBadge(imageVector = it)
            }
        },
        trailingContent = {
            // ListItem merges this into the row, so the switch state is announced with the title.
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
    )
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single
) {
    SettingsListItem(
        title = title,
        subtitle = subtitle,
        position = position,
        enabled = enabled,
        onClick = onClick,
        leadingContent = imageVector?.let {
            {
                SettingsIconBadge(imageVector = it)
            }
        },
        trailingContent = trailingIcon?.let {
            {
                SettingsNavigationChevron(icon = it)
            }
        }
    )
}

@Composable
fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    imageVector: ImageVector? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single,
    indicator: (@Composable () -> Unit)? = null
) {
    SettingsListItem(
        title = title,
        subtitle = subtitle,
        position = position,
        enabled = enabled,
        onClick = onClick,
        leadingContent = imageVector?.let {
            {
                SettingsIconBadge(imageVector = it)
            }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                indicator?.invoke()
                SettingsNavigationChevron()
            }
        }
    )
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
                    horizontal = SettingsSpacing.rowHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    SettingsIconBadge(imageVector = icon)
                    Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                }

                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = settingsContentAlpha(enabled, SettingsColors.itemContent),
                    modifier = Modifier.weight(1f)
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.space4))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = settingsContentAlpha(enabled, SettingsColors.supportingText),
                    modifier = Modifier.padding(start = if (icon != null) Spacing.space48 else Spacing.space0)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space8))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = !expanded },
                modifier = Modifier.padding(start = if (icon != null) Spacing.space48 else Spacing.space0)
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
                    horizontal = SettingsSpacing.rowHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = SettingsTypography.itemTitle,
                        color = settingsContentAlpha(enabled, SettingsColors.itemContent)
                    )

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(Spacing.space4))
                        Text(
                            text = subtitle,
                            style = SettingsTypography.itemSubtitle,
                            color = settingsContentAlpha(enabled, SettingsColors.supportingText)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.space16))

                Text(
                    text = valueLabel(value),
                    style = SettingsTypography.itemSubtitle.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space8))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
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
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = SettingsTypography.sectionHeader,
        color = SettingsColors.sectionTitle,
        modifier = modifier.padding(
            start = Spacing.space8,
            end = SettingsSpacing.itemHorizontalPadding,
            top = Spacing.space8,
            bottom = Spacing.space4
        )
    )
}
