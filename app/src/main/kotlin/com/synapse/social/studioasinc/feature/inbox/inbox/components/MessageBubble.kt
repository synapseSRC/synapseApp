package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.AutoDownloadManager
import com.synapse.social.studioasinc.core.util.DownloadManager
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.ui.settings.MediaType
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.theme.*
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.feature.inbox.inbox.components.FileMessageBubble
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun RepliesIndicatorRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.chat_reply_count, count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f).padding(start = Spacing.space8))
    }
}

@Composable
fun DateDividerChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space8),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Sizes.BorderThin
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space4),
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
        val waveLength = Sizes.MessageWaveLength.toPx()
        val amplitude = Sizes.MessageWaveAmplitude.toPx()

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
fun UnreadDividerRow(
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WavyDivider(modifier = Modifier.weight(1f).height(Sizes.WavyDividerHeight), color = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(if (count == 1) R.string.chat_divider_unread_one else R.string.chat_divider_unread_other, count),
            modifier = Modifier.padding(horizontal = Spacing.space16),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        WavyDivider(modifier = Modifier.weight(1f).height(Sizes.WavyDividerHeight), color = MaterialTheme.colorScheme.primary)
    }
}

enum class GroupPosition {
    SINGLE, FIRST, MIDDLE, LAST
}

fun isWithinTimeThreshold(timeStr1: String?, timeStr2: String?): Boolean {
    if (timeStr1 == null || timeStr2 == null) return false
    val t1 = ChatMapper.parseInstant(timeStr1)?.toEpochMilliseconds() ?: return false
    val t2 = ChatMapper.parseInstant(timeStr2)?.toEpochMilliseconds() ?: return false
    return abs(t1 - t2) <= 300_000L
}

private fun formatMessageTime(isoTimestamp: String?): String {
    if (isoTimestamp == null) return ""
    val instant = com.synapse.social.studioasinc.shared.util.TimestampFormatter.parseInstant(isoTimestamp) ?: return ""
    val local = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val hour = local.hour
    val minute = local.minute.toString().padStart(2, '0')
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:$minute $amPm"
}

@Composable
private fun ColumnScope.MessageMetadataRow(
    message: Message,
    isFromMe: Boolean,
    contentColor: Color,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    readTint: Color = if (isSystemInDarkTheme()) DarkStatusRead else StatusRead,
    currentUserId: String = ""
) {
    Row(
        modifier = modifier
            .align(Alignment.End)
            .padding(top = Spacing.space4),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (message.isEdited || message.contentStatus == ContentStatus.EDITED) {
            Text(
                text = stringResource(id = R.string.edited),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(end = Spacing.space4)
            )
        }

        Text(
            text = formatMessageTime(message.sentAt ?: message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f)
        )

        if (isFromMe && message.contentStatus != ContentStatus.DELETED && !message.isDeleted) {
            Spacer(modifier = Modifier.width(Spacing.space4))
            val statusModifier = if (onRetryClick != null && message.deliveryStatus == DeliveryStatus.FAILED) {
                Modifier.clickable { onRetryClick() }
            } else {
                Modifier
            }
            val (icon, description, tint) = when (message.deliveryStatus) {
                DeliveryStatus.SENDING -> Triple(Icons.Filled.Timer, stringResource(R.string.msg_status_sending), contentColor.copy(alpha = 0.7f))
                DeliveryStatus.FAILED -> Triple(Icons.Filled.Warning, stringResource(R.string.msg_status_failed), MaterialTheme.colorScheme.error)
                DeliveryStatus.SENT -> Triple(Icons.Filled.Check, stringResource(R.string.msg_status_sent), contentColor.copy(alpha = 0.7f))
                DeliveryStatus.DELIVERED -> Triple(Icons.Filled.DoneAll, stringResource(R.string.msg_status_delivered), contentColor.copy(alpha = 0.7f))
                DeliveryStatus.READ -> Triple(Icons.Filled.DoneAll, stringResource(R.string.msg_status_read), readTint)
            }
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = statusModifier.size(Sizes.IconSemiSmall)
            )
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

    // A message whose payload could not be decrypted must never fall through to the
    // type-specific renderers, which would print the raw envelope as ordinary text.
    if (message.isUndecryptableMessage()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(Sizes.CornerMedium),
                tonalElevation = Sizes.BorderThin
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8)) {
                    UndecryptableMessageNotice(fontScale = fontScale, contentColor = contentColor)
                    MessageMetadataRow(
                        message = message,
                        isFromMe = isFromMe,
                        contentColor = contentColor,
                        onRetryClick = onRetryClick
                    )
                }
            }
        }
        return
    }

    // SYSTEM messages render as centered pill without tails or metadata status row
    if (message.messageType == MessageType.SYSTEM) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.space8),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = Sizes.BorderThin
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // CALL messages get their own direction-aligned bubble, so no nested bubble surface
    if (message.messageType == MessageType.CALL) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.space4),
            contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            CallLogCardBubble(
                metadata = message.metadataContainer?.call,
                isFromMe = isFromMe,
                cornerRadius = cornerRadius,
                modifier = Modifier.widthIn(max = bubbleMaxWidth),
                footer = { footerColor ->
                    MessageMetadataRow(
                        message = message,
                        isFromMe = isFromMe,
                        contentColor = footerColor,
                        onRetryClick = onRetryClick
                    )
                }
            )
        }
        return
    }

    // STICKER messages render image directly without an outer bubble background surface fill
    if (message.messageType == MessageType.STICKER) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.space4),
            contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
                message.mediaUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Sticker",
                        modifier = Modifier
                            .sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
                            .clip(RoundedCornerShape(Sizes.CornerMedium))
                            .clickable { onClick() }
                    )
                }
                MessageMetadataRow(
                    message = message,
                    isFromMe = isFromMe,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onRetryClick = onRetryClick
                )
            }
        }
        return
    }

    // GIF messages render inline with a thin outline border directly against the screen surface
    if (message.messageType == MessageType.GIF) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.space4),
            contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
                message.mediaUrl?.let { url ->
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = Color.Transparent,
                        modifier = Modifier
                            .widthIn(max = bubbleMaxWidth)
                            .heightIn(max = Sizes.HeightExtraLarge)
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Animated GIF",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .clickable { onClick() }
                        )
                    }
                }
                MessageMetadataRow(
                    message = message,
                    isFromMe = isFromMe,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onRetryClick = onRetryClick
                )
            }
        }
        return
    }

    val radius = cornerRadius.dp
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { Sizes.AvatarDefault.toPx() }

    val isMediaBubble = message.messageType in listOf(
        MessageType.IMAGE,
        MessageType.VIDEO,
        MessageType.MEDIA_GROUP,
        MessageType.LOCATION,
        MessageType.LIVE_LOCATION,
        MessageType.MAP
    )

    val standardShape = remember(position, isFromMe, radius) {
        object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density
            ): androidx.compose.ui.graphics.Outline {
                val sharpCorner = lerp(Sizes.CornerSharp, radius, (offsetX.value / threshold).coerceIn(0f, 1f))
                // A group starts with both of its top corners at the full bubble radius; only the
                // corner facing the next bubble in the run stays tight.
                val delegate = if (isFromMe) {
                    when (position) {
                        GroupPosition.SINGLE -> RoundedCornerShape(radius)
                        GroupPosition.FIRST -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomEnd = sharpCorner, bottomStart = radius)
                        GroupPosition.MIDDLE -> RoundedCornerShape(topStart = radius, topEnd = sharpCorner, bottomEnd = sharpCorner, bottomStart = radius)
                        GroupPosition.LAST -> RoundedCornerShape(topStart = radius, topEnd = sharpCorner, bottomEnd = radius, bottomStart = radius)
                    }
                } else {
                    when (position) {
                        GroupPosition.SINGLE -> RoundedCornerShape(radius)
                        GroupPosition.FIRST -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomEnd = radius, bottomStart = sharpCorner)
                        GroupPosition.MIDDLE -> RoundedCornerShape(topStart = sharpCorner, topEnd = radius, bottomEnd = radius, bottomStart = sharpCorner)
                        GroupPosition.LAST -> RoundedCornerShape(topStart = sharpCorner, topEnd = radius, bottomEnd = radius, bottomStart = radius)
                    }
                }
                return delegate.createOutline(size, layoutDirection, density)
            }
        }
    }

    val shape = if (isMediaBubble) RoundedCornerShape(28.dp) else standardShape

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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
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
                    Spacer(modifier = Modifier.width(Spacing.space8))
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
                        tonalElevation = if (hasReplyPreview) Sizes.BorderThin else Sizes.ElevationNone
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
                                        .padding(horizontal = Spacing.space8, vertical = Spacing.space4)
                                        .widthIn(max = bubbleMaxWidth),
                                    shape = RoundedCornerShape((radius - Sizes.CornerMedium).coerceAtLeast(Sizes.CornerSmall)),
                                    onClick = { message.replyToId?.let(onQuoteClick) }
                                )
                                Spacer(modifier = Modifier.height(Spacing.space4))
                            }

                            if ((message.isForwarded || message.forwardedFromMessageId != null) && !isDeletedMessage) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = Spacing.space4)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Forwarded",
                                        tint = contentColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(Sizes.IconSemiSmall)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.space4))
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
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = shape,
                                    tonalElevation = Sizes.BorderThin
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Block,
                                                contentDescription = stringResource(R.string.cd_deleted_message),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(Sizes.IconSemiSmall).padding(end = Spacing.space4)
                                            )
                                            Text(
                                                text = stringResource(R.string.msg_deleted),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontStyle = FontStyle.Italic,
                                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
                                                )
                                            )
                                        }
                                        MessageMetadataRow(
                                            message = message,
                                            isFromMe = isFromMe,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            onRetryClick = onRetryClick
                                        )
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
                                            shape = RoundedCornerShape(28.dp),
                                            modifier = placeholderModifier.clickable { userRequestedDownload = true }
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(Spacing.space16)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Tap to download media",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(Sizes.IconLarge)
                                                )
                                                Spacer(modifier = Modifier.height(Spacing.space8))
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
                                                    model = coil3.request.ImageRequest.Builder(LocalContext.current)
                                                        .data(message.mediaUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = stringResource(R.string.cd_post_image),
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = Sizes.HeightExtraLarge)
                                                        .clip(RoundedCornerShape(28.dp))
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
                                        Spacer(modifier = Modifier.height(Spacing.space4))
                                        Surface(
                                            color = if (hasReplyPreview) Color.Transparent else containerColor,
                                            contentColor = contentColor,
                                            shape = if (hasReplyPreview) androidx.compose.ui.graphics.RectangleShape else shape,
                                            tonalElevation = if (hasReplyPreview) Sizes.ElevationNone else Sizes.BorderThin
                                        ) {
                                            Text(
                                                text = message.content,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = contentColor,
                                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
                                                ),
                                                modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8)
                                            )
                                        }
                                }
                                MessageMetadataRow(
                                    message = message,
                                    isFromMe = isFromMe,
                                    contentColor = contentColor,
                                    onRetryClick = onRetryClick
                                )
                            } else {
                                Surface(
                                    color = if (hasReplyPreview) Color.Transparent else containerColor,
                                    contentColor = contentColor,
                                    shape = if (hasReplyPreview) androidx.compose.ui.graphics.RectangleShape else shape,
                                    tonalElevation = if (hasReplyPreview) Sizes.ElevationNone else Sizes.BorderThin
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8),
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
                                                            modifier = Modifier.height(Sizes.AudioFallbackRowHeight)
                                                        ) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(Sizes.IconSemiMedium),
                                                                color = contentColor,
                                                                strokeWidth = Sizes.BorderDefault
                                                            )
                                                            Spacer(modifier = Modifier.width(Spacing.space8))
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
                                                        onRetryClick = onRetryClick
                                                    )
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
                                                                override fun onSuccess(savedUri: Uri, fileName: String) {}
                                                                override fun onProgress(progress: Int) {}
                                                                override fun onError(error: String) {}
                                                            }
                                                        )
                                                    },
                                                    openUrl = { url -> uriHandler.openUri(url) }
                                                )
                                                MessageMetadataRow(
                                                    message = message,
                                                    isFromMe = isFromMe,
                                                    contentColor = contentColor,
                                                    onRetryClick = onRetryClick
                                                )
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

                                            MessageType.EPHEMERAL_MEDIA -> {
                                                Column {
                                                    ViewOnceCardBubble(metadata = message.metadataContainer?.viewOnce, mediaUrl = message.mediaUrl, contentColor = contentColor, onOpenEphemeralMedia = onOpenEphemeralMedia)
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
                                                                modifier = Modifier.padding(bottom = Spacing.space8)
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
                                                            onRetryClick = onRetryClick
                                                    )
                                                }
                                            }
                                            }
                                        }
                                }
                            }
                        }
                    }

                }
            }

            if (reactions.isNotEmpty() || replyCount > 0) {
                Row(
                    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
                ) {
                    if (!isFromMe && showAvatar) {
                        Spacer(modifier = Modifier.width(Sizes.AvatarSmall + Spacing.space8))
                    }
                    Column(
                        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
                    ) {
                        if (reactions.isNotEmpty()) {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.padding(top = Spacing.space4),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.space4, if (isFromMe) Alignment.End else Alignment.Start),
                                verticalArrangement = Arrangement.spacedBy(Spacing.space4)
                            ) {
                                reactions.forEach { (emoji, count) ->
                                    val type = SharedReactionType.values().find { it.emoji == emoji } ?: SharedReactionType.LIKE
                                    Box(
                                        modifier = Modifier
                                            .sizeIn(minWidth = AccessibilitySizes.minimumTouchTarget)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = false, radius = 24.dp),
                                                role = Role.Button,
                                                onClick = { onReactionSelected(type) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (message.userReaction == type)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            tonalElevation = Sizes.ElevationLow
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
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
                        }

                        if (replyCount > 0) {
                            RepliesIndicatorRow(count = replyCount)
                        }
                    }
                }
            }
        }
    }
}
