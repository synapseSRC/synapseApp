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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import androidx.compose.ui.res.stringResource
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
fun UnreadDividerRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        Text(
            text = stringResource(if (count == 1) R.string.chat_divider_unread_one else R.string.chat_divider_unread_other, count),
            modifier = Modifier.padding(horizontal = Spacing.Medium),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
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
    onReactionSelected: (SharedReactionType) -> Unit = {},
    getLinkMetadataUseCase: GetLinkMetadataUseCase? = null,
    fontScale: Float = 1.0f,
    cornerRadius: Int = 16,
    themePreset: ChatThemePreset = ChatThemePreset.DEFAULT
) {
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart

    val containerColor = if (isFromMe) {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.primary
            ChatThemePreset.OCEAN -> LightPrimaryContainer
            ChatThemePreset.FOREST -> ForestBubbleBackground
            ChatThemePreset.SUNSET -> SunsetBubbleBackground
            ChatThemePreset.MONOCHROME -> Gray200
        }
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = if (isFromMe) {
        when (themePreset) {
            ChatThemePreset.DEFAULT -> MaterialTheme.colorScheme.onPrimary
            ChatThemePreset.OCEAN -> DarkPrimaryContainer
            ChatThemePreset.FOREST -> ForestBubbleText
            ChatThemePreset.SUNSET -> SunsetBubbleText
            ChatThemePreset.MONOCHROME -> Gray900
        }
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    // UI logic applied carefully matching sender side for sharpness:
    val radius = androidx.compose.ui.unit.Dp(cornerRadius.toFloat())
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

        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .combinedClickable(

                onLongClick = onLongClick
,
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
        contentAlignment = alignment
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = Sizes.BorderThin,
            modifier = Modifier.widthIn(max = Sizes.BubbleMaxWidth)
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)) {

                if (replyToMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(Sizes.CornerMedium),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.ExtraSmall)
                    ) {
                        Column(modifier = Modifier.padding(Spacing.Small)) {
                            Text(
                                text = if (replyToMessage.senderId == message.senderId) "You" else "Them",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = replyToMessage.content ?: "",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
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
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                ) {
                    if (message.expiresAt != null) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Disappearing Message",
                            modifier = Modifier.size(Sizes.StatusDot),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }
                    if (message.isEncrypted) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "End-to-End Encrypted",
                            modifier = Modifier.size(Sizes.StatusDot),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }
                    if (!message.isEncrypted && message.encryptionFailureReason != null) {
                        var showTooltip by remember { mutableStateOf(false) }
                        Box {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Not Encrypted",
                                modifier = Modifier
                                    .size(Sizes.IconSmall)
                                    .clickable { showTooltip = !showTooltip },
                                tint = MaterialTheme.colorScheme.error
                            )
                            if (showTooltip) {
                                DropdownMenu(
                                    expanded = showTooltip,
                                    onDismissRequest = { showTooltip = false }
                                ) {
                                    Text(
                                        text = message.encryptionFailureReason ?: "Encryption failed",
                                        modifier = Modifier.padding(Spacing.Small),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    if (message.isEdited) {
                        Text(
                            text = stringResource(id = R.string.edited),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Text(
                        text = formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                    if (isFromMe) {
                        val isRead = message.deliveryStatus == DeliveryStatus.READ
                        val isSent = message.deliveryStatus == DeliveryStatus.SENT
                        val iconTint = if (isRead) StatusRead else contentColor.copy(alpha = 0.6f)
                        if (isSent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = iconTint,
                                modifier = Modifier.size(Sizes.IconSemiSmall)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = if (isRead) "Read" else "Delivered",
                                tint = iconTint,
                                modifier = Modifier.size(Sizes.IconSemiSmall)
                            )
                        }
                    }
                }
            }
        }
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
