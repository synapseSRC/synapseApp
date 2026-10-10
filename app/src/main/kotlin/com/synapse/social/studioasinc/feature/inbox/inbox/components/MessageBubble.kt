package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import android.net.Uri
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.AutoDownloadManager
import com.synapse.social.studioasinc.core.util.DownloadManager
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.ui.settings.MediaType
import androidx.compose.material.icons.filled.Download
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.theme.*
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.feature.inbox.inbox.components.FileMessageBubble
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun RepliesIndicatorRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.chat_reply_count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f).padding(start = Spacing.Small))
    }
}

@Composable
fun SenderHeaderRow(
    avatarUrl: String?,
    displayName: String,
    timestamp: String,
    isStarred: Boolean,
    showAvatar: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Sender Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(Sizes.AvatarSmall)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
        }
        Column {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isStarred) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Starred",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DateDividerChip(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(Sizes.CornerDefault),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = Sizes.BorderThin
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WavyDivider(modifier: Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val path = Path()
        val waveLength = Spacing.Large.toPx()
        val amplitude = Spacing.Tiny.toPx()

        path.moveTo(0f, size.height / 2f)
        var currentX = 0f
        while (currentX < size.width) {
            path.cubicTo(
                currentX + waveLength * 0.3642f, size.height / 2f - amplitude * 1.5f,
                currentX + waveLength * 0.6358f, size.height / 2f + amplitude * 1.5f,
                currentX + waveLength, size.height / 2f
            )
            currentX += waveLength
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = Sizes.BorderDefault.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun UnreadDividerRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WavyDivider(modifier = Modifier.weight(1f).height(Spacing.ExtraSmallMedium), color = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(if (count == 1) R.string.chat_divider_unread_one else R.string.chat_divider_unread_other, count),
            modifier = Modifier.padding(horizontal = Spacing.Medium),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        WavyDivider(modifier = Modifier.weight(1f).height(Spacing.ExtraSmallMedium), color = MaterialTheme.colorScheme.primary)
    }
}

enum class GroupPosition {
    SINGLE, FIRST, MIDDLE, LAST
}

fun isWithinTimeThreshold(timeStr1: String?, timeStr2: String?): Boolean {
    if (timeStr1 == null || timeStr2 == null) return false
    return try {
        val t1 = Instant.parse(timeStr1).epochSecond
        val t2 = Instant.parse(timeStr2).epochSecond
        abs(t1 - t2) <= 300
    } catch (e: Exception) {
        false
    }
}

private fun formatMessageTime(isoTimestamp: String?): String {
    if (isoTimestamp == null) return ""
    return try {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun ColumnScope.MessageMetadataRow(
    message: Message,
    isFromMe: Boolean,
    contentColor: Color,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    currentUserId: String = ""
) {
    Row(
        modifier = modifier
            .align(if (isFromMe) Alignment.End else Alignment.Start)
            .padding(top = Spacing.ExtraSmall),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (message.isEdited || message.contentStatus == ContentStatus.EDITED) {
            Text(
                text = stringResource(id = R.string.edited),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(end = Spacing.ExtraSmall)
            )
        }

        Text(
            text = formatMessageTime(message.sentAt ?: message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f)
        )

        if (isFromMe && message.contentStatus != ContentStatus.DELETED && !message.isDeleted) {
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            val statusModifier = if (onRetryClick != null && message.deliveryStatus == DeliveryStatus.FAILED) {
                Modifier.clickable { onRetryClick() }
            } else {
                Modifier
            }
            when (message.deliveryStatus) {
                DeliveryStatus.SENDING -> {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = stringResource(R.string.msg_status_sending),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(Sizes.IconSemiSmall)
                    )
                }
                DeliveryStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.msg_status_failed),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = statusModifier.size(Sizes.IconSemiSmall)
                    )
                }
                DeliveryStatus.SENT -> {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.msg_status_sent),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(Sizes.IconSemiSmall)
                    )
                }
                DeliveryStatus.DELIVERED -> {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = stringResource(R.string.msg_status_delivered),
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(Sizes.IconSemiSmall)
                    )
                }
                DeliveryStatus.READ -> {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = stringResource(R.string.msg_status_read),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Sizes.IconSemiSmall)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    position: GroupPosition = GroupPosition.SINGLE,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onSwipeToReply: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    replyToMessage: Message? = null,
    onLongClick: () -> Unit = {},
    onReactionSelected: (SharedReactionType) -> Unit = {},
    getLinkMetadataUseCase: GetLinkMetadataUseCase? = null,
    fontScale: Float = 1.0f,
    cornerRadius: Int = 16,
    themePreset: ChatThemePreset = ChatThemePreset.DEFAULT,
    showAvatar: Boolean = true,
    senderName: String? = null,
    senderAvatarUrl: String? = null,
    replyToSenderName: String? = null,
    reactions: List<Pair<String, Int>> = emptyList(),
    replyCount: Int = 0,
    onQuoteClick: (messageId: String) -> Unit = {},
        onOpenBatchGallery: (Message) -> Unit = {},
    onVoteOption: (pollId: String, optionId: String) -> Unit = { _, _ -> },
    onOpenPost: (String) -> Unit = {},
    onOpenStory: (String) -> Unit = {},
    onOpenEvent: (String) -> Unit = {},
    onOpenProduct: (String) -> Unit = {},
    onOpenEphemeralMedia: (String) -> Unit = {},
    onOpenMap: (Double, Double) -> Unit = { _, _ -> },
    onContactAction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    currentUserId: String = ""
) {
    val configuration = LocalConfiguration.current
    val bubbleMaxWidth = (configuration.screenWidthDp.dp * 0.78f).coerceIn(Sizes.ChatMaxWidth, Sizes.BubbleMaxWidth)

    val isDark = isSystemInDarkTheme()

    val containerColor = if (isFromMe) {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primaryContainer
            ChatThemePreset.OCEAN -> if (isDark) OceanDarkContainer else OceanLightContainer
            ChatThemePreset.FOREST -> if (isDark) ForestDarkContainer else ForestLightContainer
            ChatThemePreset.SUNSET -> if (isDark) SunsetDarkContainer else SunsetLightContainer
            ChatThemePreset.MONOCHROME -> if (isDark) MonochromeDarkContainer else MonochromeLightContainer
        }
    } else {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    }

    val contentColor = if (isFromMe) {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.onPrimaryContainer
            ChatThemePreset.OCEAN -> if (isDark) OceanDarkOnContainer else OceanLightOnContainer
            ChatThemePreset.FOREST -> if (isDark) ForestDarkOnContainer else ForestLightOnContainer
            ChatThemePreset.SUNSET -> if (isDark) SunsetDarkOnContainer else SunsetLightOnContainer
            ChatThemePreset.MONOCHROME -> if (isDark) MonochromeDarkOnContainer else MonochromeLightOnContainer
        }
    } else {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    val radius = cornerRadius.dp
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { Sizes.AvatarDefault.toPx() }
    val shape = remember(position, isFromMe, radius) {
        object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density
            ): androidx.compose.ui.graphics.Outline {
                val sharpCorner = lerp(Sizes.CornerSharp, radius, (offsetX.value / threshold).coerceIn(0f, 1f))
                val delegate = if (isFromMe) {
                    when (position) {
                        GroupPosition.SINGLE -> RoundedCornerShape(radius, radius, radius, radius)
                        GroupPosition.FIRST -> RoundedCornerShape(radius, radius, sharpCorner, radius)
                        GroupPosition.MIDDLE -> RoundedCornerShape(radius, sharpCorner, sharpCorner, radius)
                        GroupPosition.LAST -> RoundedCornerShape(radius, sharpCorner, radius, radius)
                    }
                } else {
                    when (position) {
                        GroupPosition.SINGLE -> RoundedCornerShape(radius, radius, radius, radius)
                        GroupPosition.FIRST -> RoundedCornerShape(radius, radius, radius, sharpCorner)
                        GroupPosition.MIDDLE -> RoundedCornerShape(sharpCorner, radius, radius, sharpCorner)
                        GroupPosition.LAST -> RoundedCornerShape(sharpCorner, radius, radius, radius)
                    }
                }
                return delegate.createOutline(size, layoutDirection, density)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(
                top = if (position == GroupPosition.FIRST || position == GroupPosition.SINGLE) Spacing.Small else Spacing.None,
                bottom = Spacing.Tiny
            )
            .offset { IntOffset(offsetX.value.toInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value >= threshold) {
                                onSwipeToReply()
                            }
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount > 0 || offsetX.value > 0) {
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount * 0.5f).coerceIn(0f, threshold * 1.5f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    }
                )
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isFromMe && showAvatar) {
                if (position == GroupPosition.LAST || position == GroupPosition.SINGLE) {
                    com.synapse.social.studioasinc.feature.shared.components.UserAvatar(
                        avatarUrl = senderAvatarUrl,
                        displayName = senderName,
                        size = Sizes.AvatarSmall
                    )
                } else {
                    Spacer(modifier = Modifier.size(Sizes.AvatarSmall))
                }
                Spacer(modifier = Modifier.width(Spacing.Small))
            }
            Column(
                horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
            ) {
                if (!isFromMe && (position == GroupPosition.FIRST || position == GroupPosition.SINGLE) && senderName != null) {
                    SenderHeaderRow(
                        avatarUrl = senderAvatarUrl,
                        displayName = senderName,
                        timestamp = formatMessageTime(message.sentAt ?: message.createdAt),
                        isStarred = false,
                        showAvatar = false
                    )
                }

                Column(
                    horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
                ) {
                    val isDeletedMessage = message.isDeleted || message.contentStatus == ContentStatus.DELETED
                    val isBatchMedia = message.messageType == MessageType.MEDIA_GROUP || message.attachments.size >= 2
                    val isMedia = !isDeletedMessage && (isBatchMedia || message.messageType == MessageType.IMAGE || message.messageType == MessageType.VIDEO)
                    val uriHandler = LocalUriHandler.current
                    val context = LocalContext.current

                    val hasReplyPreview = !message.replyToId.isNullOrBlank() && !isDeletedMessage
                    Surface(
                        modifier = Modifier.widthIn(max = bubbleMaxWidth),
                        color = if (hasReplyPreview) containerColor else Color.Transparent,
                        contentColor = contentColor,
                        shape = if (hasReplyPreview) shape else androidx.compose.ui.graphics.RectangleShape,
                        tonalElevation = if (hasReplyPreview) Sizes.BorderThin else Spacing.None
                    ) {
                        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
                            if (hasReplyPreview) {
                                QuotedMessagePreview(
                                    originalMessage = replyToMessage,
                                    originalMessageId = message.replyToId,
                                    currentUserId = currentUserId,
                                    originalSenderName = replyToSenderName,
                                    fontScale = fontScale,
                                    modifier = Modifier
                                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
                                        .widthIn(max = bubbleMaxWidth),
                                    shape = RoundedCornerShape((radius - Spacing.Small).coerceAtLeast(Sizes.CornerSmall)),
                                    onClick = { message.replyToId?.let(onQuoteClick) }
                                )
                                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                            }

                            if ((message.isForwarded || message.forwardedFromMessageId != null) && !isDeletedMessage) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = Spacing.ExtraSmall)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Forwarded",
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(Sizes.IconSemiSmall)
                                )
                                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                                Text(
                                    text = "Forwarded",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (isDeletedMessage) {
                            Surface(
                                color = containerColor.copy(alpha = 0.6f),
                                contentColor = contentColor,
                                shape = shape,
                                tonalElevation = Sizes.BorderThin
                            ) {
                                Column(modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = stringResource(R.string.cd_deleted_message),
                                            tint = contentColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(Sizes.IconSemiSmall).padding(end = Spacing.ExtraSmall)
                                        )
                                        Text(
                                            text = stringResource(R.string.msg_deleted),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = contentColor.copy(alpha = 0.6f),
                                                fontStyle = FontStyle.Italic,
                                                fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
                                            )
                                        )
                                    }
                                    MessageMetadataRow(
                                        message = message,
                                        isFromMe = isFromMe,
                                        contentColor = contentColor,
                                        onRetryClick = onRetryClick)
                                }
                            }
                        } else if (isMedia) {
                            val mediaType = when (message.messageType) {
                                MessageType.VIDEO -> MediaType.VIDEO
                                MessageType.AUDIO -> MediaType.AUDIO
                                else -> MediaType.PHOTO
                            }
                            var isAutoDownloadAllowed by remember(message.id) { mutableStateOf<Boolean?>(null) }
                            var userRequestedDownload by remember(message.id) { mutableStateOf(false) }

                            val settingsRepo = remember(context) {
                                com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl.getInstance(context.applicationContext)
                            }

                            LaunchedEffect(message.id) {
                                settingsRepo.autoDownloadRules.collect { rules ->
                                    isAutoDownloadAllowed = AutoDownloadManager.isAutoDownloadAllowed(context, rules, mediaType)
                                }
                            }

                            val shouldShowMedia = isFromMe || (isAutoDownloadAllowed == true) || userRequestedDownload

                            BoxWithConstraints(
                                modifier = if (isBatchMedia) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()
                            ) {
                                if (!shouldShowMedia) {
                                    val placeholderModifier = if (isBatchMedia) {
                                        val dimensions = stackedMediaCardDimensions(maxWidth)
                                        Modifier
                                            .width(dimensions.stackWidth)
                                            .height(dimensions.stackHeight)
                                    } else {
                                        Modifier
                                            .fillMaxWidth()
                                            .height(Sizes.HeightMediaSingle)
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(Sizes.CornerMedium),
                                        modifier = placeholderModifier.clickable { userRequestedDownload = true }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(Spacing.Medium)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Tap to download media",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(Sizes.IconLarge)
                                            )
                                            Spacer(modifier = Modifier.height(Spacing.Small))
                                            Text(
                                                text = "Tap to load media",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    when {
                                        isBatchMedia -> {
                                            StackedMediaCard(
                                                attachments = message.attachments,
                                                onClick = { onOpenBatchGallery(message) }
                                            )
                                        }
                                        message.messageType == MessageType.IMAGE -> {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                    .data(message.mediaUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = stringResource(R.string.cd_post_image),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = Sizes.HeightExtraLarge)
                                                    .clip(RoundedCornerShape(Sizes.CornerMedium))
                                                    .clickable {
                                                        message.mediaUrl?.let { uriHandler.openUri(it) }
                                                    }
                                            )
                                        }
                                        message.messageType == MessageType.VIDEO -> {
                                            message.mediaUrl?.let {
                                                VideoPlayerBox(mediaUrl = it)
                                            }
                                        }
                                    }
                                }
                            }

                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                Surface(
                                    color = if (hasReplyPreview) Color.Transparent else containerColor,
                                    contentColor = contentColor,
                                    shape = if (hasReplyPreview) androidx.compose.ui.graphics.RectangleShape else shape,
                                    tonalElevation = if (hasReplyPreview) Spacing.None else Sizes.BorderThin
                                ) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = contentColor,
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
                                        ),
                                        modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                                    )
                                }
                            }
                            MessageMetadataRow(
                                message = message,
                                isFromMe = isFromMe,
                                contentColor = contentColor,
                                onRetryClick = onRetryClick)
                        } else {
                            Surface(
                                color = if (hasReplyPreview) Color.Transparent else containerColor,
                                contentColor = contentColor,
                                shape = if (hasReplyPreview) androidx.compose.ui.graphics.RectangleShape else shape,
                                tonalElevation = if (hasReplyPreview) Spacing.None else Sizes.BorderThin
                            ) {
                                Column(modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Small)) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = Spacing.Small),
                                        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
                                    ) {
                                        when (message.messageType) {
                                            MessageType.VOICE, MessageType.AUDIO -> {
                                                Column {
                                                    val audioUrl = message.mediaUrl
                                                    if (!audioUrl.isNullOrBlank()) {
                                                        val attachmentDuration = message.attachments.firstOrNull()?.duration?.toLong()?.let { it * 1000L } ?: 0L
                                                        VoiceMessagePlayer(
                                                            mediaUrl = audioUrl,
                                                            tintColor = contentColor,
                                                            isFromMe = isFromMe,
                                                            initialDurationMs = attachmentDuration
                                                        )
                                                    } else {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .height(Spacing.ExtraLarge)
                                                                .padding(horizontal = Spacing.Small)
                                                        ) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(Sizes.IconSemiMedium),
                                                                color = contentColor,
                                                                strokeWidth = Sizes.BorderDefault
                                                            )
                                                            Spacer(modifier = Modifier.width(Spacing.Small))
                                                            Text(
                                                                text = message.content.ifBlank { stringResource(R.string.chat_media_audio_voice) },
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = contentColor
                                                            )
                                                        }
                                                    }
                                                    MessageMetadataRow(
                                                        message = message,
                                                        isFromMe = isFromMe,
                                                        contentColor = contentColor,
                                                        onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.FILE -> {
                                                FileMessageBubble(
                                                    message = message,
                                                    contentColor = contentColor,
                                                    downloadFile = { url, fileName ->
                                                        DownloadManager.downloadFile(
                                                            context,
                                                            url,
                                                            fileName,
                                                            object : DownloadManager.DownloadCallback {
                                                                override fun onSuccess(savedUri: Uri, fileName: String) {
                                                                    // File saved successfully
                                                                }

                                                                override fun onProgress(progress: Int) {
                                                                    // Download progress
                                                                }

                                                                override fun onError(error: String) {
                                                                    // Handle download error
                                                                }
                                                            }
                                                        )
                                                    },
                                                    openUrl = { url -> uriHandler.openUri(url) }
                                                )
                                                MessageMetadataRow(
                                                    message = message,
                                                    isFromMe = isFromMe,
                                                    contentColor = contentColor,
                                                    onRetryClick = onRetryClick)
                                            }


                                            MessageType.CONTACT -> {
                                                Column {
                                                    ContactCardBubble(metadata = message.metadataContainer?.contact, contentColor = contentColor, onContactAction = onContactAction)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.LOCATION -> {
                                                Column {
                                                    LocationCardBubble(metadata = message.metadataContainer?.location, contentColor = contentColor, onOpenMap = onOpenMap)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.MAP -> {
                                                Column {
                                                    MapCardBubble(metadata = message.metadataContainer?.map, contentColor = contentColor, onOpenMap = onOpenMap)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.LIVE_LOCATION -> {
                                                Column {
                                                    LiveLocationCardBubble(metadata = message.metadataContainer?.liveLocation, contentColor = contentColor)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.POLL -> {
                                                Column {
                                                    PollCardBubble(metadata = message.metadataContainer?.poll, messageId = message.id, contentColor = contentColor, onVoteOption = onVoteOption)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.MUSIC -> {
                                                Column {
                                                    MusicCardBubble(metadata = message.metadataContainer?.music, contentColor = contentColor)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.SHARED_POST -> {
                                                Column {
                                                    SharedPostCardBubble(metadata = message.metadataContainer?.sharedPost, contentColor = contentColor, onOpenPost = onOpenPost)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.STORY_SHARE -> {
                                                Column {
                                                    SharedStoryCardBubble(metadata = message.metadataContainer?.sharedStory, contentColor = contentColor, onOpenStory = onOpenStory)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.EVENT -> {
                                                Column {
                                                    EventCardBubble(metadata = message.metadataContainer?.event, contentColor = contentColor, onOpenEvent = onOpenEvent)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.PRODUCT -> {
                                                Column {
                                                    ProductCardBubble(metadata = message.metadataContainer?.product, contentColor = contentColor, onOpenProduct = onOpenProduct)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.PAYMENT -> {
                                                Column {
                                                    PaymentCardBubble(metadata = message.metadataContainer?.payment, contentColor = contentColor)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.CODE_SNIPPET -> {
                                                Column {
                                                    CodeSnippetCardBubble(metadata = message.metadataContainer?.codeSnippet, contentColor = contentColor)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.CALL -> {
                                                Column {
                                                    CallLogCardBubble(metadata = message.metadataContainer?.call, contentColor = contentColor)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.EPHEMERAL_MEDIA -> {
                                                Column {
                                                    ViewOnceCardBubble(metadata = message.metadataContainer?.viewOnce, mediaUrl = message.mediaUrl, contentColor = contentColor, onOpenEphemeralMedia = onOpenEphemeralMedia)
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }

                                            MessageType.GIF, MessageType.STICKER -> {
                                                Column {
                                                    message.mediaUrl?.let { url ->
                                                        AsyncImage(
                                                            model = url,
                                                            contentDescription = "Sticker or GIF",
                                                            modifier = Modifier.size(150.dp).padding(4.dp)
                                                        )
                                                    }
                                                    MessageMetadataRow(message = message, isFromMe = isFromMe, contentColor = contentColor, onRetryClick = onRetryClick)
                                                }
                                            }
                            else -> {
                                                Column {
                                                    val firstUrl = UrlUtils.extractFirstUrl(message.content)
                                                    if (firstUrl != null && getLinkMetadataUseCase != null) {
                                                        LinkPreviewCard(
                                                            url = firstUrl,
                                                            useCase = getLinkMetadataUseCase,
                                                            modifier = Modifier.padding(bottom = Spacing.Small)
                                                        )
                                                    }

                                                    val annotatedString = buildAnnotatedString {
                                                        val urlRegex = Regex("(https?://[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)|(www\\.[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=%]+)")
                                                        var lastIndex = 0

                                                        urlRegex.findAll(message.content).forEach { matchResult ->
                                                            append(message.content.substring(lastIndex, matchResult.range.first))

                                                            val url = matchResult.value
                                                            val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                                "https://$url"
                                                            } else {
                                                                url
                                                            }

                                                            pushStringAnnotation(tag = "URL", annotation = fullUrl)
                                                            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                                                append(url)
                                                            }
                                                            pop()

                                                            lastIndex = matchResult.range.last + 1
                                                        }
                                                        append(message.content.substring(lastIndex))
                                                    }

                                                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                                    Text(
                                                        text = annotatedString,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = contentColor,
                                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                                                        ),
                                                        onTextLayout = { layoutResult = it },
                                                        modifier = Modifier.pointerInput(Unit) {
                                                            detectTapGestures(
                                                                onLongPress = { onLongClick() },
                                                                onTap = { pos ->
                                                                    var urlHandled = false
                                                                    layoutResult?.let { lResult ->
                                                                        val offset = lResult.getOffsetForPosition(pos)
                                                                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                                                            .firstOrNull()?.let { annotation ->
                                                                                IntentUtils.openUrl(context, annotation.item)
                                                                                urlHandled = true
                                                                            }
                                                                    }
                                                                    if (!urlHandled) onClick()
                                                                }
                                                            )
                                                        }
                                                    )
                                                    MessageMetadataRow(
                                                        message = message,
                                                        isFromMe = isFromMe,
                                                        contentColor = contentColor,
                                                        onRetryClick = onRetryClick)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier
                            .padding(top = Spacing.Tiny)
                            .padding(horizontal = Spacing.ExtraSmall),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Tiny, if (isFromMe) Alignment.End else Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Tiny)
                    ) {
                        reactions.forEach { (emoji, count) ->
                            val type = SharedReactionType.values().find { it.emoji == emoji } ?: SharedReactionType.LIKE
                            Surface(
                                shape = CircleShape,
                                color = if (message.userReaction == type)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = Spacing.Tiny,
                                modifier = Modifier.clickable { onReactionSelected(type) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.ExtraSmallMedium, vertical = Spacing.Tiny),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.Tiny)
                                ) {
                                    Text(text = emoji, fontSize = FontSizes.Small)
                                    if (count > 1) {
                                        Text(
                                            text = count.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = FontSizes.Small
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (replyCount > 0) {
                    RepliesIndicatorRow(count = replyCount)
                }
            }
        }
    }
}
