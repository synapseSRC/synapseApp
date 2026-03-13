package com.synapse.social.studioasinc.ui.settings


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.domain.model.ChatListLayout
import com.synapse.social.studioasinc.domain.model.ChatSwipeGesture
import com.synapse.social.studioasinc.domain.model.ChatThemePreset
import com.synapse.social.studioasinc.domain.model.WallpaperType

import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

import com.synapse.social.studioasinc.feature.shared.theme.*

private const val MAX_BLUR_RADIUS = 50f


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    viewModel: ChatSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val chatFontScale by viewModel.chatFontScale.collectAsState()
    val chatMessageCornerRadius by viewModel.chatMessageCornerRadius.collectAsState()
    val chatThemePreset by viewModel.chatThemePreset.collectAsState()
    val chatWallpaperType by viewModel.chatWallpaperType.collectAsState()
    val chatListLayout by viewModel.chatListLayout.collectAsState()
    val chatSwipeGesture by viewModel.chatSwipeGesture.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ChatLivePreview(
                    fontScale = chatFontScale,
                    cornerRadius = chatMessageCornerRadius,
                    themePreset = chatThemePreset,
                    wallpaperType = chatWallpaperType,
                    wallpaperValue = viewModel.chatWallpaperValue.collectAsState().value,
                    blurIntensity = viewModel.chatWallpaperBlur.collectAsState().value
                )
            }

            item {
                SettingsBlock(title = "Appearance") {
                    ThemeModePicker(
                        selectedMode = themeMode,
                        onModeSelected = { viewModel.updateThemeMode(it) }
                    )
                }
            }

            item {
                SettingsBlock(title = "Message Text Size") {
                    Text(
                        text = "Size: ${(16 * chatFontScale).toInt()}sp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Slider(
                        value = chatFontScale,
                        onValueChange = { viewModel.updateChatFontScale(it) },
                        valueRange = 0.75f..1.875f,
                        steps = 8,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                SettingsBlock(title = "Color Theme") {
                    ThemePicker(
                        selectedTheme = chatThemePreset,
                        onThemeSelected = { viewModel.updateChatThemePreset(it) }
                    )
                }
            }

            item {
                SettingsBlock(title = "Message Corners") {
                    Text(
                        text = "Radius: ${chatMessageCornerRadius}dp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Slider(
                        value = chatMessageCornerRadius.toFloat(),
                        onValueChange = { viewModel.updateChatMessageCornerRadius(it.toInt()) },
                        valueRange = 0f..24f,
                        steps = 23,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }


            item {
                SettingsBlock(title = "Chat Background") {
                    WallpaperPicker(
                        selectedWallpaper = chatWallpaperType,
                        onWallpaperSelected = { viewModel.updateChatWallpaperType(it) },
                        selectedWallpaperValue = viewModel.chatWallpaperValue.collectAsState().value,
                        onWallpaperValueSelected = { viewModel.updateChatWallpaperValue(it) },
                        blurIntensity = viewModel.chatWallpaperBlur.collectAsState().value,
                        onBlurIntensityChanged = { viewModel.updateChatWallpaperBlur(it) }
                    )
                }
            }


            item {
                SettingsBlock(title = "Chat List View") {
                    ChatListViewPicker(
                        selectedLayout = chatListLayout,
                        onLayoutSelected = { viewModel.updateChatListLayout(it) }
                    )
                }
            }

            item {
                SettingsBlock(title = "Swipe Gestures") {
                    SwipeGesturePicker(
                        selectedGesture = chatSwipeGesture,
                        onGestureSelected = { viewModel.updateChatSwipeGesture(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatLivePreview(
    fontScale: Float,
    cornerRadius: Int,
    themePreset: ChatThemePreset,
    wallpaperType: WallpaperType,
    wallpaperValue: String?,
    blurIntensity: Float
) {
    val backgroundColor = when (wallpaperType) {
        WallpaperType.SOLID_COLOR -> Gray200
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val myBubbleColor = when (themePreset) {
        ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primaryContainer
        ChatThemePreset.OCEAN -> LightPrimaryContainer
        ChatThemePreset.FOREST -> Color(0xFFE8F5E9)
        ChatThemePreset.SUNSET -> Color(0xFFFBE9E7)
        ChatThemePreset.MONOCHROME -> Gray200
    }

    val myTextColor = when (themePreset) {
        ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.onPrimaryContainer
        ChatThemePreset.OCEAN -> DarkPrimaryContainer
        ChatThemePreset.FOREST -> Color(0xFF1B5E20)
        ChatThemePreset.SUNSET -> Color(0xFFBF360C)
        ChatThemePreset.MONOCHROME -> Gray900
    }

    val theirBubbleColor = MaterialTheme.colorScheme.surface
    val theirTextColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
    ) {
        if (wallpaperType == WallpaperType.DEFAULT) {
            val mContext = LocalContext.current
            AsyncImage(
                model = mContext.resources.getIdentifier("pattern_11", "raw", mContext.packageName),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(radius = (blurIntensity * MAX_BLUR_RADIUS).dp),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        } else if ((wallpaperType == WallpaperType.PATTERN || wallpaperType == WallpaperType.PRESET_IMAGE) && wallpaperValue != null) {
            val context = LocalContext.current
            val resId = context.resources.getIdentifier(wallpaperValue.substringBeforeLast("."), "raw", context.packageName)
            if (resId != 0) {
                 AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resId)
                        .apply {
                            if (wallpaperType == WallpaperType.PATTERN) {
                                decoderFactory(SvgDecoder.Factory())
                            }
                        }
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(radius = (blurIntensity * MAX_BLUR_RADIUS).dp),
                    contentScale = ContentScale.Crop
                 )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Their message
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = cornerRadius.dp,
                                topEnd = cornerRadius.dp,
                                bottomEnd = cornerRadius.dp,
                                bottomStart = 4.dp
                            )
                        )
                        .background(theirBubbleColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Text(
                        text = "Do you know what time it is?",
                        fontSize = (16 * fontScale).sp,
                        color = theirTextColor
                    )
                }
            }
            // My message
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = cornerRadius.dp,
                                topEnd = cornerRadius.dp,
                                bottomStart = cornerRadius.dp,
                                bottomEnd = 4.dp
                            )
                        )
                        .background(myBubbleColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Text(
                        text = "It's morning in Tokyo 🗼",
                        fontSize = (16 * fontScale).sp,
                        color = myTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsBlock(
    title: String,
    content: @Composable
ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun ThemePicker(
    selectedTheme: ChatThemePreset,
    onThemeSelected: (ChatThemePreset) -> Unit
) {
    val themes = ChatThemePreset.entries.toList()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        themes.forEach { theme ->
            val color = when (theme) {
                ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primary
                ChatThemePreset.OCEAN -> AccentBlue
                ChatThemePreset.FOREST -> StatusOnline
                ChatThemePreset.SUNSET -> Color(0xFFFF5722)
                ChatThemePreset.MONOCHROME -> Color(0xFF9E9E9E)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onThemeSelected(theme) }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (theme == selectedTheme) 3.dp else 0.dp,
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
private fun WallpaperPicker(
    selectedWallpaper: WallpaperType,
    onWallpaperSelected: (WallpaperType) -> Unit,
    selectedWallpaperValue: String?,
    onWallpaperValueSelected: (String?) -> Unit,
    blurIntensity: Float,
    onBlurIntensityChanged: (Float) -> Unit
) {
    Column {
        WallpaperTypeSelector(selectedWallpaper, onWallpaperSelected)

        if (selectedWallpaper == WallpaperType.PATTERN || selectedWallpaper == WallpaperType.PRESET_IMAGE) {
            PatternSelector(selectedWallpaper, selectedWallpaperValue, onWallpaperValueSelected)
        }

        if (selectedWallpaper == WallpaperType.PATTERN || selectedWallpaper == WallpaperType.PRESET_IMAGE || selectedWallpaper == WallpaperType.DEFAULT) {
            BlurSlider(blurIntensity, onBlurIntensityChanged)
        }
    }
}

@Composable
private fun WallpaperTypeSelector(
    selectedWallpaper: WallpaperType,
    onWallpaperSelected: (WallpaperType) -> Unit
) {
    val wallpapers = WallpaperType.entries.toList()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        wallpapers.forEach { wallpaper ->
            val isSelected = wallpaper == selectedWallpaper
            val bgColor = when(wallpaper) {
                WallpaperType.SOLID_COLOR -> Color(0xFFE0E0E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onWallpaperSelected(wallpaper) }
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
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
private fun PatternSelector(
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

    Text("Select Resource", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedWallpaperValue

            val resId = context.resources.getIdentifier(item, "raw", context.packageName)

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onWallpaperValueSelected(item) }
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
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
                     Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                }
            }
        }
    }
}

@Composable
private fun BlurSlider(
    blurIntensity: Float,
    onBlurIntensityChanged: (Float) -> Unit
) {
    Text(
        text = "Blur Intensity: ${(blurIntensity * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    var localBlur by remember(blurIntensity) { mutableStateOf(blurIntensity) }
    Slider(
        value = localBlur,
        onValueChange = { localBlur = it },
        onValueChangeFinished = { onBlurIntensityChanged(localBlur) },
        valueRange = 0f..1f,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
@Composable
private fun ChatListViewPicker(
    selectedLayout: ChatListLayout,
    onLayoutSelected: (ChatListLayout) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        ChatListLayout.entries.forEach { layout ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLayoutSelected(layout) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = layout == selectedLayout,
                    onClick = { onLayoutSelected(layout) }
                )
                Spacer(modifier = Modifier.width(16.dp))
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
private fun ThemeModePicker(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = mode == selectedMode,
                    onClick = { onModeSelected(mode) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = mode.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun SwipeGesturePicker(
    selectedGesture: ChatSwipeGesture,
    onGestureSelected: (ChatSwipeGesture) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        ChatSwipeGesture.entries.forEach { gesture ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGestureSelected(gesture) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gesture == selectedGesture,
                    onClick = { onGestureSelected(gesture) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = gesture.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
