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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.*
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
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        themes.forEach { theme ->
            val color = when (theme) {
                ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primary
                ChatThemePreset.OCEAN -> AccentBlue
                ChatThemePreset.FOREST -> StatusOnline
                ChatThemePreset.SUNSET -> SunsetAccent
                ChatThemePreset.MONOCHROME -> StatusOffline
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onThemeSelected(theme) }
            ) {
                Box(
                    modifier = Modifier
                        .size(Sizes.IconGiant)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (theme == selectedTheme) Sizes.BorderSelected else 0.dp,
                            color = if (theme == selectedTheme) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (theme == selectedTheme) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(Sizes.IconLarge)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = theme.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (theme == selectedTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    Column {
        WallpaperTypeSelector(selectedWallpaper, onWallpaperSelected)

        if (selectedWallpaper == WallpaperType.SOLID_COLOR) {
            SolidColorSelector(selectedWallpaperValue, onWallpaperValueSelected)
        }

        if (selectedWallpaper == WallpaperType.PATTERN || selectedWallpaper == WallpaperType.PRESET_IMAGE) {
            PatternSelector(selectedWallpaper, selectedWallpaperValue, onWallpaperValueSelected)
        }

        if (selectedWallpaper == WallpaperType.PATTERN || selectedWallpaper == WallpaperType.PRESET_IMAGE || selectedWallpaper == WallpaperType.DEFAULT) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        wallpapers.forEach { wallpaper ->
            val isSelected = wallpaper == selectedWallpaper
            val bgColor = when(wallpaper) {
                WallpaperType.SOLID_COLOR -> Gray200
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onWallpaperSelected(wallpaper) }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = Sizes.WidthLarge, height = Sizes.WidthExtraLarge)
                        .clip(RoundedCornerShape(Sizes.CornerMedium))
                        .background(bgColor)
                        .border(
                            width = if (isSelected) Sizes.BorderSelected else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Sizes.IconLarge)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = wallpaper.name.lowercase().replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun SolidColorSelector(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit
) {
    val colors = listOf(
        "#E0E0E0", "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9",
        "#C5CAE9", "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB",
        "#C8E6C9", "#DCEDC8", "#F0F4C3", "#FFF9C4", "#FFECB3",
        "#FFE0B2", "#FFCCBC", "#D7CCC8", "#F5F5F5", "#CFD8DC"
    )

    Text(
        text = "Select Color",
        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        style = MaterialTheme.typography.labelLarge
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        colors.forEach { colorHex ->
            val isSelected = colorHex == selectedColor
            Box(
                modifier = Modifier
                    .size(Sizes.WidthLarge)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                    .clickable { onColorSelected(colorHex) }
                    .border(
                        width = if (isSelected) Sizes.BorderSelected else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (Color(android.graphics.Color.parseColor(colorHex)).luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                }
            }
        }
    }
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

    Text(stringResource(R.string.label_select_resource), modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small), style = MaterialTheme.typography.labelLarge)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedWallpaperValue

            val resId = context.resources.getIdentifier(item, "raw", context.packageName)

            Box(
                modifier = Modifier
                    .size(Sizes.WidthLarge)
                    .clip(RoundedCornerShape(Sizes.CornerMedium))
                    .clickable { onWallpaperValueSelected(item) }
                    .border(
                        width = if (isSelected) Sizes.BorderSelected else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(Sizes.CornerMedium)
                    )
            ) {
                if (resId != 0) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(resId)
                            .apply {
                                if (isPattern) {
                                    decoderFactory(coil.decode.SvgDecoder.Factory())
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
            }
        }
    }
}

@Composable
internal fun BlurSlider(
    blurIntensity: Float,
    onBlurIntensityChanged: (Float) -> Unit
) {
    Text(
        text = "Blur Intensity: ${(blurIntensity * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
    )
    var localBlur by remember(blurIntensity) { mutableStateOf(blurIntensity) }
    Slider(
        value = localBlur,
        onValueChange = { localBlur = it },
        onValueChangeFinished = { onBlurIntensityChanged(localBlur) },
        valueRange = 0f..1f,
        modifier = Modifier.padding(horizontal = Spacing.Medium)
    )
}

@Composable
internal fun ChatListViewPicker(
    selectedLayout: ChatListLayout,
    onLayoutSelected: (ChatListLayout) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = Spacing.ExtraSmall)) {
        ChatListLayout.entries.forEach { layout ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLayoutSelected(layout) }
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = layout == selectedLayout,
                    onClick = { onLayoutSelected(layout) }
                )
                Spacer(modifier = Modifier.width(Spacing.Medium))
                Column {
                    Text(
                        text = if (layout == ChatListLayout.DOUBLE_LINE) "Two Lines" else "One Line",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (layout == ChatListLayout.DOUBLE_LINE) "Shows a snippet of the last message." else "More compact, fits more chats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun SwipeGesturePicker(
    selectedGesture: ChatSwipeGesture,
    onGestureSelected: (ChatSwipeGesture) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = Spacing.ExtraSmall)) {
        ChatSwipeGesture.entries.forEach { gesture ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGestureSelected(gesture) }
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gesture == selectedGesture,
                    onClick = { onGestureSelected(gesture) }
                )
                Spacer(modifier = Modifier.width(Spacing.Medium))
                Text(
                    text = gesture.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

internal fun String?.toColor(): Color {
    return try {
        if (this != null && this.startsWith("#")) {
            Color(android.graphics.Color.parseColor(this))
        } else {
            Gray200
        }
    } catch (e: Exception) {
        Gray200
    }
}

internal fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
