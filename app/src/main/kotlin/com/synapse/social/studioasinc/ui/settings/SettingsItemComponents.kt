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
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

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
        modifier = modifier.size(AppSpacing.iconBadgeSize),
        shape = AppShapes.iconBadgeShape,
        color = AppColors.iconContainerBackground,
        contentColor = AppColors.itemIcon
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(AppSpacing.rowIconSize)
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
        modifier = modifier.size(AppSpacing.rowIconSize),
        tint = AppColors.chevronIcon
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
        .heightIn(min = AppSpacing.rowMinHeight)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }

    val contentPadding = PaddingValues(
        start = AppSpacing.rowHorizontalPadding,
        end = AppSpacing.rowHorizontalPadding,
        top = AppSpacing.rowVerticalPadding,
        bottom = AppSpacing.rowVerticalPadding
    )

    val headline: @Composable () -> Unit = {
        Text(
            text = title,
            style = AppTypography.itemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    val supporting: (@Composable () -> Unit)? = subtitle?.let { text ->
        {
            Text(
                text = text,
                style = AppTypography.itemSubtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    val colors = ListItemDefaults.colors(
        containerColor = AppColors.cardBackground,
        contentColor = AppColors.itemContent,
        leadingContentColor = AppColors.itemIcon,
        trailingContentColor = AppColors.chevronIcon,
        supportingContentColor = AppColors.supportingText,
        disabledContentColor = AppColors.itemContent.copy(alpha = AppColors.disabledContentAlpha),
        disabledSupportingContentColor = AppColors.supportingText.copy(alpha = AppColors.disabledContentAlpha)
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
    if (enabled) content else content.copy(alpha = AppColors.disabledContentAlpha)

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
        color = AppColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $dropdownDescription, $selectedOption"
                }
                .padding(
                    horizontal = AppSpacing.rowHorizontalPadding,
                    vertical = AppSpacing.itemVerticalPadding
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    SettingsIconBadge(imageVector = icon)
                    Spacer(modifier = Modifier.width(AppSpacing.iconTextSpacing))
                }

                Text(
                    text = title,
                    style = AppTypography.itemTitle,
                    color = settingsContentAlpha(enabled, AppColors.itemContent),
                    modifier = Modifier.weight(1f)
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.space4))
                Text(
                    text = subtitle,
                    style = AppTypography.itemSubtitle,
                    color = settingsContentAlpha(enabled, AppColors.supportingText),
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
                    shape = AppShapes.inputShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = AppTypography.itemSubtitle
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
        color = AppColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title, $sliderDescription, ${valueLabel(value)}"
                }
                .padding(
                    horizontal = AppSpacing.rowHorizontalPadding,
                    vertical = AppSpacing.itemVerticalPadding
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
                        style = AppTypography.itemTitle,
                        color = settingsContentAlpha(enabled, AppColors.itemContent)
                    )

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(Spacing.space4))
                        Text(
                            text = subtitle,
                            style = AppTypography.itemSubtitle,
                            color = settingsContentAlpha(enabled, AppColors.supportingText)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.space16))

                Text(
                    text = valueLabel(value),
                    style = AppTypography.itemSubtitle.copy(fontWeight = FontWeight.SemiBold),
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
                horizontal = AppSpacing.itemHorizontalPadding,
                vertical = AppSpacing.itemVerticalPadding
            ),
        shape = AppShapes.itemShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isDestructive) AppColors.destructiveButton
                           else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isDestructive) AppColors.destructiveText
                          else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = title,
            style = AppTypography.buttonText
        )
    }
}

