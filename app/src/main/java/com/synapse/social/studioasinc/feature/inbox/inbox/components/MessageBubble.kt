package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.theme.*
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
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
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun RepliesIndicatorRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count replies",
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
    isStarred: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Sender Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
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
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Starred",
            tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        val waveLength = 12.dp.toPx()
        val amplitude = 3.dp.toPx()
        var currentX = 0f
        path.moveTo(0f, size.height / 2f)
        while (currentX < size.width) {
            path.relativeQuadraticTo(waveLength / 4f, -amplitude, waveLength / 2f, 0f)
            path.relativeQuadraticTo(waveLength / 4f, amplitude, waveLength / 2f, 0f)
            currentX += waveLength
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 1.5.dp.toPx(),
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
        WavyDivider(modifier = Modifier.weight(1f).height(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        Text(
            text = stringResource(if (count == 1) R.string.chat_divider_unread_one else R.string.chat_divider_unread_other, count),
            modifier = Modifier.padding(horizontal = Spacing.Medium),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        WavyDivider(modifier = Modifier.weight(1f).height(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
        abs(t1 - t2) <= 5 * 60 // 5 minutes
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    position: GroupPosition = GroupPosition.SINGLE,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onSwipeToReply: () -> Unit = {},
    replyToMessage: Message? = null,
    onLongClick: () -> Unit = {},
    onShowReactionPicker: () -> Unit = {},
    onReactionSelected: (SharedReactionType) -> Unit = {},
    getLinkMetadataUseCase: GetLinkMetadataUseCase? = null,
    fontScale: Float = 1.0f,
    cornerRadius: Int = 16,
    themePreset: ChatThemePreset = ChatThemePreset.DEFAULT,
    showAvatar: Boolean = true,
    senderName: String? = null,
    senderAvatarUrl: String? = null,
    reactions: List<Pair<String, Int>> = emptyList(),
    replyCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start

    val isDark = isSystemInDarkTheme()

    val containerColor = if (isFromMe) {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primaryContainer
            ChatThemePreset.OCEAN -> if (isDark) DarkPrimaryContainer else LightPrimaryContainer
            ChatThemePreset.FOREST -> if (isDark) ForestBubbleText else ForestBubbleBackground
            ChatThemePreset.SUNSET -> if (isDark) SunsetBubbleText else SunsetBubbleBackground
            ChatThemePreset.MONOCHROME -> if (isDark) Gray700 else Gray200
        }
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val contentColor = if (isFromMe) {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.onPrimaryContainer
            ChatThemePreset.OCEAN -> if (isDark) DarkOnPrimaryContainer else DarkPrimaryContainer
            ChatThemePreset.FOREST -> if (isDark) ForestBubbleBackground else ForestBubbleText
            ChatThemePreset.SUNSET -> if (isDark) SunsetBubbleBackground else SunsetBubbleText
            ChatThemePreset.MONOCHROME -> if (isDark) Gray200 else Gray900
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // UI logic applied carefully matching sender side for sharpness:
    val radius = cornerRadius.dp
    val shape = if (isFromMe) {
        when (position) {
            GroupPosition.SINGLE -> RoundedCornerShape(radius, radius, radius, radius)
            GroupPosition.FIRST -> RoundedCornerShape(radius, radius, Sizes.CornerSharp, radius)
            GroupPosition.MIDDLE -> RoundedCornerShape(radius, Sizes.CornerSharp, Sizes.CornerSharp, radius)
            GroupPosition.LAST -> RoundedCornerShape(radius, Sizes.CornerSharp, radius, radius)
        }
    } else {
        when (position) {
            GroupPosition.SINGLE -> RoundedCornerShape(radius, radius, radius, radius)
            GroupPosition.FIRST -> RoundedCornerShape(radius, radius, radius, Sizes.CornerSharp)
            GroupPosition.MIDDLE -> RoundedCornerShape(Sizes.CornerSharp, radius, radius, Sizes.CornerSharp)
            GroupPosition.LAST -> RoundedCornerShape(Sizes.CornerSharp, radius, radius, radius)
        }
    }

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { Sizes.AvatarDefault.toPx() }



    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = { onToggleSelection() },
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
                        if (dragAmount > 0 || offsetX.value > 0) { // Only swipe right
                            change.consume()
                            coroutineScope.launch {
                                // Add some resistance
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
            if (!isFromMe) {
                if (showAvatar && (position == GroupPosition.LAST || position == GroupPosition.SINGLE)) {
                    AsyncImage(
                        model = senderAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(Sizes.AvatarSmall)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Spacer(modifier = Modifier.size(Sizes.AvatarSmall))
                }
                Spacer(modifier = Modifier.width(Spacing.Small))
            }
            Column(
                horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
            ) {
        if (position == GroupPosition.FIRST || position == GroupPosition.SINGLE) {
            Text(
                text = remember(message.createdAt) { formatMessageTime(message.createdAt) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = Spacing.Tiny)
            )
        }
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = Sizes.BorderThin,
            modifier = Modifier.widthIn(max = Sizes.BubbleMaxWidth)
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)) {

                if (replyToMessage != null) {
                    val quoteCardColor = if (isFromMe)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    Surface(
                        color = quoteCardColor,
                        shape = RoundedCornerShape(Sizes.CornerMedium),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.Small)
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.Small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❝",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = Spacing.Small)
                            )
                            val isOwnReply = replyToMessage.senderId == message.senderId
                            AsyncImage(
                                model = if (isOwnReply) null else senderAvatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(Sizes.AvatarTiny)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                            Column {
                                Text(
                                    text = if (isOwnReply) stringResource(R.string.chat_reply_you)
                                           else (senderName?.uppercase() ?: stringResource(R.string.chat_reply_them)),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = replyToMessage.content ?: "",
                                    style = TextStyle(
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val uriHandler = LocalUriHandler.current
                val context = LocalContext.current

                when (message.messageType) {
                    MessageType.IMAGE -> {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Image message",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = Sizes.HeightExtraLarge)
                                    .clip(RoundedCornerShape(Sizes.CornerMedium))
                                    .clickable {
                                        message.mediaUrl?.let { uriHandler.openUri(it) }
                                    }
                            )
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        }
                        MessageType.VIDEO -> {
                            message.mediaUrl?.let {
                                VideoPlayerBox(mediaUrl = it)
                            }
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        }
                        MessageType.AUDIO -> {
                            message.mediaUrl?.let {
                                AudioPlayer(mediaUrl = it, tintColor = contentColor)
                            }
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        }
                        MessageType.FILE -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = Spacing.ExtraSmall).clickable {
                                    message.mediaUrl?.let { uriHandler.openUri(it) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = stringResource(R.string.chat_cd_file),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(Spacing.Small))
                                Text(
                                    text = (message.content ?: "").ifEmpty { stringResource(R.string.chat_media_attachment) },
                                    color = contentColor,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        }
                        else -> {
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

                                urlRegex.findAll(message.content ?: "").forEach { matchResult ->
                                    append((message.content ?: "").substring(lastIndex, matchResult.range.first))

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
                                append((message.content ?: "").substring(lastIndex))
                            }

                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            Text(
                                text = annotatedString,
                                style = androidx.compose.ui.text.TextStyle(
                                    color = contentColor,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                                ),
                                onTextLayout = { layoutResult = it },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            onLongClick()
                                        },
                                        onTap = { pos ->
                                            layoutResult?.let { layoutResult ->
                                                val offset = layoutResult.getOffsetForPosition(pos)
                                                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                                    .firstOrNull()?.let { annotation ->
                                                        IntentUtils.openUrl(context, annotation.item)
                                                    }
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.Tiny))
                    if (message.isEdited) {
                        Text(
                            text = stringResource(id = R.string.edited),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        if (isFromMe && (position == GroupPosition.LAST || position == GroupPosition.SINGLE)
            && message.deliveryStatus == DeliveryStatus.READ) {
            AsyncImage(
                model = senderAvatarUrl,
                contentDescription = stringResource(R.string.chat_cd_reader_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = Spacing.Tiny)
                    .size(Sizes.AvatarTiny)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = Spacing.Small)
                    .offset(y = (-8).dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Tiny)
            ) {
                message.reactions.forEach { (type, count) ->
                    Surface(
                        shape = CircleShape,
                        color = if (message.userReaction == type)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.clickable { onReactionSelected(type) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(text = type.emoji, fontSize = 12.sp)
                            if (count > 1) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onShowReactionPicker,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddReaction,
                        contentDescription = "Add Reaction",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (replyCount > 0) {
            RepliesIndicatorRow(count = replyCount)
        }
        } // Column
        } // Row
    }
}

/**
 * Formats an ISO timestamp to a short time string (e.g., "2:30 PM").
 */
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
