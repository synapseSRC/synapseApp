package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.*
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType

@Composable
internal fun ThemePicker(
    selectedTheme: ChatThemePreset,
    onThemeSelected: (ChatThemePreset) -> Unit
) {
    val themes = ChatThemePreset.entries.toList()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        themes.forEach { theme ->
            val isSelected = theme == selectedTheme
            val color = when (theme) {
                ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primary
                ChatThemePreset.OCEAN -> AccentBlue
                ChatThemePreset.FOREST -> StatusOnline
                ChatThemePreset.SUNSET -> SunsetAccent
                ChatThemePreset.MONOCHROME -> StatusOffline
            }

            val themeName = theme.displayName()
            val themeDescription = if (isSelected) "Selected $themeName theme" else "$themeName theme"

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onThemeSelected(theme) }
                    )
                    .semantics(mergeDescendants = true) {
                        role = Role.RadioButton
                        contentDescription = themeDescription
                    }
                    .padding(vertical = Spacing.ExtraSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val iconTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(Spacing.Large)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.Small))
                Text(
                    text = themeName,
                    style = SettingsTypography.itemSubtitle,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun WallpaperPicker(
    selectedWallpaper: WallpaperType,
    onWallpaperSelected: (WallpaperType) -> Unit,
    selectedWallpaperValue: String?,
    onWallpaperValueSelected: (String?) -> Unit,
    blurIntensity: Float,
    onBlurIntensityChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        WallpaperTypeSelector(selectedWallpaper, onWallpaperSelected)

        if (selectedWallpaper == WallpaperType.SOLID_COLOR) {
            HorizontalDivider(
                color = SettingsColors.divider,
                thickness = Sizes.BorderThin,
                modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
            )
            SolidColorSelector(selectedWallpaperValue, onWallpaperValueSelected)
        }

        if (selectedWallpaper == WallpaperType.PATTERN || selectedWallpaper == WallpaperType.PRESET_IMAGE) {
            HorizontalDivider(
                color = SettingsColors.divider,
                thickness = Sizes.BorderThin,
                modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
            )
            PatternSelector(selectedWallpaper, selectedWallpaperValue, onWallpaperValueSelected)
        }

        if (selectedWallpaper == WallpaperType.PATTERN || selectedWallpaper == WallpaperType.PRESET_IMAGE || selectedWallpaper == WallpaperType.DEFAULT) {
            HorizontalDivider(
                color = SettingsColors.divider,
                thickness = Sizes.BorderThin,
                modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
            )
            BlurSlider(blurIntensity, onBlurIntensityChanged)
        }
    }
}

@Composable
internal fun WallpaperTypeSelector(
    selectedWallpaper: WallpaperType,
    onWallpaperSelected: (WallpaperType) -> Unit
) {
    val wallpapers = WallpaperType.entries.toList()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SettingsSpacing.itemVerticalPadding)
    ) {
        Text(
            text = stringResource(R.string.settings_chat_background_title),
            style = SettingsTypography.itemTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
        )
        Spacer(modifier = Modifier.height(Spacing.SmallMedium))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SettingsSpacing.itemHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            wallpapers.forEach { wallpaper ->
                val isSelected = wallpaper == selectedWallpaper
                val wallpaperName = wallpaper.name.lowercase().replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                val bgColor = when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    wallpaper == WallpaperType.SOLID_COLOR -> SettingsColors.iconContainerBackground
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onWallpaperSelected(wallpaper) }
                        )
                        .semantics(mergeDescendants = true) {
                            role = Role.RadioButton
                            contentDescription = if (isSelected) "Selected $wallpaperName" else wallpaperName
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 80.dp)
                            .clip(SettingsShapes.inputShape)
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = SettingsShapes.inputShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Spacing.Large)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Text(
                        text = wallpaperName,
                        style = SettingsTypography.itemSubtitle,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun SolidColorSelector(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit
) {
    var showColorPickerPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#E0E0E0", "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9",
        "#C5CAE9", "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB",
        "#C8E6C9", "#DCEDC8", "#F0F4C3", "#FFF9C4", "#FFECB3",
        "#FFE0B2", "#FFCCBC", "#D7CCC8", "#F5F5F5", "#CFD8DC"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SettingsSpacing.itemVerticalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsSpacing.itemHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_select_color),
                style = SettingsTypography.itemTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = { showColorPickerPicker = true }
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    modifier = Modifier.size(Spacing.MediumLarge)
                )
                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                Text("Custom Color")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.SmallMedium))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SettingsSpacing.itemHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            // Custom Color Wheel Swatch
            val isCustomSelected = selectedColor != null && selectedColor !in presetColors
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCustomSelected) safeParseColorHex(selectedColor, MaterialTheme.colorScheme.primary)
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .clickable { showColorPickerPicker = true }
                    .border(
                        width = if (isCustomSelected) 3.dp else 1.dp,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCustomSelected) {
                    val color = safeParseColorHex(selectedColor, MaterialTheme.colorScheme.primary)
                    val iconTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Custom Selected",
                        tint = iconTint,
                        modifier = Modifier.size(Spacing.Large)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Custom Color",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(Spacing.MediumLarge)
                    )
                }
            }

            presetColors.forEach { colorHex ->
                val isSelected = colorHex.equals(selectedColor, ignoreCase = true)
                val color = safeParseColorHex(colorHex, Color.Gray)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onColorSelected(colorHex) }
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val iconTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(Spacing.Large)
                        )
                    }
                }
            }
        }
    }

    if (showColorPickerPicker) {
        ExpressiveColorPickerDialog(
            initialColorHex = selectedColor ?: "#BBDEFB",
            onDismiss = { showColorPickerPicker = false },
            onColorConfirmed = { hex ->
                onColorSelected(hex)
                showColorPickerPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorConfirmed: (String) -> Unit
) {
    var hexInput by remember(initialColorHex) {
        mutableStateOf(if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex")
    }
    val parsedColor = remember(hexInput) { safeParseColorHex(hexInput, Color.Gray) }

    val extendedSwatches = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#1976D2"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Choose Chat Color",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                // Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(SettingsShapes.cardShape)
                        .background(parsedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SettingsShapes.cardShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hexInput.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (parsedColor.luminance() > 0.5f) Color.Black else Color.White
                    )
                }

                // Swatch Grid
                Text(
                    text = "Color Palette",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    extendedSwatches.forEach { swatchHex ->
                        val swatchColor = safeParseColorHex(swatchHex, Color.Gray)
                        val isSelected = hexInput.equals(swatchHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .clickable { hexInput = swatchHex }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(Spacing.Medium)
                                )
                            }
                        }
                    }
                }

                // HEX Input Field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetterOrDigit() || it == '#' }
                        hexInput = if (!filtered.startsWith("#")) "#$filtered" else filtered
                    },
                    label = { Text("HEX Code (e.g. #1976D2)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = SettingsShapes.inputShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorConfirmed(hexInput) }
            ) {
                Text("Apply Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun PatternSelector(
    selectedWallpaper: WallpaperType,
    selectedWallpaperValue: String?,
    onWallpaperValueSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    val isPattern = selectedWallpaper == WallpaperType.PATTERN
    val items = remember(isPattern) {
        try {
            val rawClass = Class.forName("${context.packageName}.R\$raw")
            rawClass.fields
                .map { it.name }
                .filter { if (isPattern) it.startsWith("pattern_") else it.startsWith("wallpaper_") }
                .sortedBy {
                    val numStr = it.substringAfterLast("_").filter { c -> c.isDigit() }
                    numStr.toIntOrNull() ?: 0
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SettingsSpacing.itemVerticalPadding)
    ) {
        Text(
            text = stringResource(R.string.label_select_resource),
            style = SettingsTypography.itemTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
        )

        Spacer(modifier = Modifier.height(Spacing.SmallMedium))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SettingsSpacing.itemHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedWallpaperValue
                val resId = context.resources.getIdentifier(item, "raw", context.packageName)

                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 64.dp)
                        .clip(SettingsShapes.inputShape)
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onWallpaperValueSelected(item) }
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = SettingsShapes.inputShape
                        )
                ) {
                    if (resId != 0) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(resId)
                                .apply {
                                    if (isPattern) {
                                        decoderFactory(SvgDecoder.Factory())
                                    }
                                }
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(Spacing.Large)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BlurSlider(
    blurIntensity: Float,
    onBlurIntensityChanged: (Float) -> Unit
) {
    var localBlur by remember(blurIntensity) { mutableStateOf(blurIntensity) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blur Intensity",
                style = SettingsTypography.itemTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(localBlur * 100).toInt()}%",
                style = SettingsTypography.itemSubtitle,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

        Slider(
            value = localBlur,
            onValueChange = { localBlur = it },
            onValueChangeFinished = { onBlurIntensityChanged(localBlur) },
            valueRange = 0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

internal fun String?.toColor(): Color {
    return safeParseColorHex(this, LightSurfaceVariant)
}

internal fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
