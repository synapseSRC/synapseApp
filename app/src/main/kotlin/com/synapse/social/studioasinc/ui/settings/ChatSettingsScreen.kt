package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatListLayout
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatSwipeGesture
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
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
    val chatWallpaperValue by viewModel.chatWallpaperValue.collectAsState()
    val chatWallpaperBlur by viewModel.chatWallpaperBlur.collectAsState()
    val chatListLayout by viewModel.chatListLayout.collectAsState()
    val chatSwipeGesture by viewModel.chatSwipeGesture.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = SettingsColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            contentPadding = PaddingValues(bottom = Spacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {
                ChatLivePreview(
                    fontScale = chatFontScale,
                    cornerRadius = chatMessageCornerRadius,
                    themePreset = chatThemePreset,
                    wallpaperType = chatWallpaperType,
                    wallpaperValue = chatWallpaperValue,
                    blurIntensity = chatWallpaperBlur
                )
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_appearance_settings_title)) {
                    val fontSizeLabel = stringResource(R.string.settings_chat_font_size_value, (16 * chatFontScale).toInt())
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_chat_font_size_title),
                        value = chatFontScale,
                        valueRange = 0.75f..1.875f,
                        steps = 8,
                        onValueChange = { viewModel.updateChatFontScale(it) },
                        valueLabel = { fontSizeLabel },
                        position = SettingsItemPosition.Top
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SettingsItemPosition.Middle.getShape(),
                        color = SettingsColors.cardBackground
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SettingsSpacing.itemVerticalPadding)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_chat_color_theme_title),
                                style = SettingsTypography.itemTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
                            )
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                            ThemePicker(
                                selectedTheme = chatThemePreset,
                                onThemeSelected = { viewModel.updateChatThemePreset(it) }
                            )
                        }
                    }

                    val cornersLabel = stringResource(R.string.settings_chat_message_corners_radius_value, chatMessageCornerRadius)
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_chat_message_corners_title),
                        value = chatMessageCornerRadius.toFloat(),
                        valueRange = 0f..24f,
                        steps = 23,
                        onValueChange = { viewModel.updateChatMessageCornerRadius(it.toInt()) },
                        valueLabel = { cornersLabel },
                        position = SettingsItemPosition.Bottom
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_chat_background_title)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SettingsItemPosition.Single.getShape(),
                        color = SettingsColors.cardBackground
                    ) {
                        WallpaperPicker(
                            selectedWallpaper = chatWallpaperType,
                            onWallpaperSelected = { viewModel.updateChatWallpaperType(it) },
                            selectedWallpaperValue = chatWallpaperValue,
                            onWallpaperValueSelected = { viewModel.updateChatWallpaperValue(it) },
                            blurIntensity = chatWallpaperBlur,
                            onBlurIntensityChanged = { viewModel.updateChatWallpaperBlur(it) }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_chat_list_view_title)) {
                    ChatRadioItem(
                        title = "One Line",
                        subtitle = "More compact, fits more chats.",
                        selected = chatListLayout == ChatListLayout.SINGLE_LINE,
                        onClick = { viewModel.updateChatListLayout(ChatListLayout.SINGLE_LINE) },
                        position = SettingsItemPosition.Top
                    )
                    ChatRadioItem(
                        title = "Two Lines",
                        subtitle = "Shows a snippet of the last message.",
                        selected = chatListLayout == ChatListLayout.DOUBLE_LINE,
                        onClick = { viewModel.updateChatListLayout(ChatListLayout.DOUBLE_LINE) },
                        position = SettingsItemPosition.Bottom
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_chat_swipe_gestures_title)) {
                    val gestures = ChatSwipeGesture.entries.toList()
                    gestures.forEachIndexed { index, gesture ->
                        val position = when {
                            gestures.size == 1 -> SettingsItemPosition.Single
                            index == 0 -> SettingsItemPosition.Top
                            index == gestures.lastIndex -> SettingsItemPosition.Bottom
                            else -> SettingsItemPosition.Middle
                        }
                        val gestureTitle = gesture.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        ChatRadioItem(
                            title = gestureTitle,
                            selected = gesture == chatSwipeGesture,
                            onClick = { viewModel.updateChatSwipeGesture(gesture) },
                            position = position
                        )
                    }
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
            .height(180.dp)
            .clip(SettingsShapes.cardShape)
            .background(backgroundColor)
    ) {
        if (wallpaperType == WallpaperType.DEFAULT) {
            val mContext = LocalContext.current
            AsyncImage(
                model = mContext.resources.getIdentifier("pattern_11", "raw", mContext.packageName),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = (blurIntensity * MAX_BLUR_RADIUS).dp),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = (blurIntensity * MAX_BLUR_RADIUS).dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SettingsSpacing.itemHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)
        ) {
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
