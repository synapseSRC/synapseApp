package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType

import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

import com.synapse.social.studioasinc.feature.shared.theme.*
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import kotlinx.datetime.Clock

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_chat_title)) },
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
            contentPadding = PaddingValues(vertical = Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
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
                SettingsBlock(title = "Message Text Size") {
                    Text(
                        text = "Size: ${(16 * chatFontScale).toInt()}sp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    )
                    Slider(
                        value = chatFontScale,
                        onValueChange = { viewModel.updateChatFontScale(it) },
                        valueRange = 0.75f..1.875f,
                        steps = 8,
                        modifier = Modifier.padding(horizontal = Spacing.Medium)
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
                        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    )
                    Slider(
                        value = chatMessageCornerRadius.toFloat(),
                        onValueChange = { viewModel.updateChatMessageCornerRadius(it.toInt()) },
                        valueRange = 0f..24f,
                        steps = 23,
                        modifier = Modifier.padding(horizontal = Spacing.Medium)
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
    val backgroundColor by animateColorAsState(
        targetValue = when (wallpaperType) {
            WallpaperType.SOLID_COLOR -> wallpaperValue.toColor()
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    val animatedFontScale by animateFloatAsState(
        targetValue = fontScale,
        animationSpec = tween(durationMillis = 300),
        label = "fontScale"
    )
    val animatedCornerRadius by animateIntAsState(
        targetValue = cornerRadius,
        animationSpec = tween(durationMillis = 300),
        label = "cornerRadius"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
            .height(Sizes.HeightExtraLarge)
            .clip(RoundedCornerShape(Sizes.CornerLarge))
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
            modifier = Modifier.fillMaxSize().padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)
        ) {
            // Their message
            MessageBubble(
                message = Message(
                    id = "preview_1",
                    chatId = "preview",
                    senderId = "other",
                    content = "Do you know what time it is?",
                    messageType = MessageType.TEXT,
                    deliveryStatus = DeliveryStatus.READ,
                    createdAt = Clock.System.now().toString()
                ),
                isFromMe = false,
                position = GroupPosition.SINGLE,
                fontScale = animatedFontScale,
                cornerRadius = animatedCornerRadius,
                themePreset = themePreset
            )

            // My message
            MessageBubble(
                message = Message(
                    id = "preview_2",
                    chatId = "preview",
                    senderId = "me",
                    content = "It's morning in Tokyo 🗼",
                    messageType = MessageType.TEXT,
                    deliveryStatus = DeliveryStatus.READ,
                    createdAt = Clock.System.now().toString()
                ),
                isFromMe = true,
                position = GroupPosition.SINGLE,
                fontScale = animatedFontScale,
                cornerRadius = animatedCornerRadius,
                themePreset = themePreset
            )
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
            .padding(vertical = Spacing.SmallMedium)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.Medium, end = Spacing.Medium, bottom = Spacing.Small)
        )
        content()
    }
}
