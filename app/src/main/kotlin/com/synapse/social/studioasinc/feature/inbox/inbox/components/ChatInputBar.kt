package com.synapse.social.studioasinc.feature.inbox.inbox.components
import androidx.compose.ui.unit.dp

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Delete
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.components.picker.SynapseFilePicker
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase

@Composable
fun ChatInputBar(
    replyingToMessage: Message?,
    editingMessage: Message?,
    smartReplies: List<String>,
    inputText: String,
    canSendMessage: Boolean,
    currentUserId: String,
    participantDisplayName: String?,
    getLinkMetadataUseCase: GetLinkMetadataUseCase?,
    context: Context,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEditing: () -> Unit,
    onUploadAndSendMedia: (filePath: String, fileName: String, contentType: String, messageType: String, caption: String?) -> Unit,
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    recordingAmplitude: Int = 0,
    onMicHeld: () -> Unit = {},
    onMicReleased: () -> Unit = {},
    onRecordingCancelled: () -> Unit = {}
) {
    var dismissedPreviewUrl by remember { mutableStateOf<String?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isSwipeToCancel by remember { mutableStateOf(false) }
    var slideOffset by remember { mutableFloatStateOf(0f) }
    var isArmedForCancel by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val cancelThresholdPx = with(LocalDensity.current) { 100.dp.toPx() }

    val micScale by animateFloatAsState(
        targetValue = if (isRecording) (if (isArmedForCancel) 1.35f else 1.2f) else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "micScale"
    )

    var isFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val inputWidthFraction by animateFloatAsState(
        targetValue = if ((isFocused && isImeVisible) || inputText.isNotEmpty()) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "inputWidthFraction"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ExtraSmall)
            .padding(top = Spacing.ExtraSmall, bottom = Spacing.Medium)
    ) {
        // Replying Header
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(Sizes.IconSemiMedium))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (replyingToMessage?.senderId == currentUserId) "Replying to yourself" else "Replying to ${participantDisplayName ?: "Them"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = replyingToMessage?.content ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(Sizes.IconLarge)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(Sizes.IconSemiMedium))
                    }
                }
            }
        }

        // Editing Header
        AnimatedVisibility(
            visible = editingMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = if (replyingToMessage == null) RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge) else RoundedCornerShape(Spacing.None),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(Sizes.IconSemiMedium))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.editing_message_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = editingMessage?.content ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onCancelEditing, modifier = Modifier.size(Sizes.IconLarge)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel edit", modifier = Modifier.size(Sizes.IconSemiMedium))
                    }
                }
            }
        }

        // Smart Replies
        AnimatedVisibility(
            visible = smartReplies.isNotEmpty() && inputText.isEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(Spacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                smartReplies.forEach { reply ->
                    AssistChip(
                        onClick = {
                            onInputTextChange(reply)
                            onSendMessage()
                            focusRequester.requestFocus()
                        },
                        label = { Text(reply) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }

        val firstUrl = UrlUtils.extractFirstUrl(inputText)
        if (firstUrl != null && firstUrl != dismissedPreviewUrl && getLinkMetadataUseCase != null) {
            LinkPreviewCard(
                url = firstUrl,
                useCase = getLinkMetadataUseCase,
                onRemove = { dismissedPreviewUrl = firstUrl },
                modifier = Modifier.padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.ExtraSmallMedium).fillMaxWidth()
            )
        }

        // Recording Indicator Row
        AnimatedVisibility(
            visible = isRecording,
            enter = slideInVertically { it } + fadeIn(tween(300)),
            exit = slideOutVertically { it } + fadeOut(tween(200))
        ) {
            val animatedSlideOffset by animateFloatAsState(targetValue = slideOffset, label = "slideOffset")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(animatedSlideOffset.toInt(), 0) }
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "recordingIndicator")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(Sizes.IconSmall)
                            .background(if (isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = alpha), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    val totalSeconds = recordingDurationMs / 1000
                    val m = totalSeconds / 60
                    val s = totalSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", m, s),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(Spacing.Medium))

                    // Waveform visualizer
                    val maxAmp = 32767f // Max amplitude for 16-bit audio
                    val normalizedAmp = (recordingAmplitude / maxAmp).coerceIn(0.1f, 1f)

                    Row(
                        modifier = Modifier.height(Sizes.IconLarge),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Tiny)
                    ) {
                        for (i in 0 until 5) {
                            val seed = remember { Random.nextFloat() * 0.5f + 0.5f }
                            val targetHeight = (Sizes.IconLarge * normalizedAmp * seed).coerceIn(Spacing.ExtraSmall, Sizes.IconLarge)
                            val animatedHeight by animateDpAsState(
                                targetValue = targetHeight,
                                animationSpec = tween(100),
                                label = "amp_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(Spacing.ExtraSmall)
                                    .height(animatedHeight)
                                    .background(
                                        if (isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(Sizes.CornerSmall)
                                    )
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                ) {
                    val cancelColor = if (isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

                    val shakeOffset = if (isArmedForCancel) {
                        val infiniteTransition = rememberInfiniteTransition(label = "shake")
                        infiniteTransition.animateFloat(
                            initialValue = -5f,
                            targetValue = 5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(50, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "shakeOffset"
                        ).value
                    } else 0f

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier
                            .size(Sizes.IconSemiMedium)
                            .offset(x = shakeOffset.dp),
                        tint = cancelColor
                    )
                    Text(
                        text = stringResource(R.string.voice_cancel_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = cancelColor,
                        modifier = Modifier.offset(x = shakeOffset.dp)
                    )
                }
            }
        }

        // Floating input row
        Surface(
            modifier = Modifier
                .fillMaxWidth(inputWidthFraction)
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(Sizes.CornerMassive),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Sizes.BorderDefault,
            shadowElevation = Spacing.ExtraSmall
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.ExtraSmall),
                verticalAlignment = Alignment.Bottom
            ) {
                // Emoji / Attachment button
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }, modifier = Modifier.size(Sizes.InputButtonCompact)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach file",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (showAttachmentMenu) {
                        SynapseFilePicker(
                            onDismissRequest = { showAttachmentMenu = false },
                            onFilesSelected = { files ->
                                files.forEach { pickedFile ->
                                    val filePath = com.synapse.social.studioasinc.core.util.FileUtils.validateAndCleanPath(context, pickedFile.uri.toString())
                                    if (filePath != null) {
                                        val type = if (pickedFile.mimeType.startsWith("video/")) "video" else if (pickedFile.mimeType.startsWith("image/")) "image" else if (pickedFile.mimeType.startsWith("audio/")) "audio" else "file"
                                        onUploadAndSendMedia(
                                            filePath,
                                            pickedFile.fileName,
                                            pickedFile.mimeType,
                                            type,
                                            null
                                        )
                                    }
                                }
                            },
                            maxSelection = 1
                        )
                    }
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Sizes.InputButtonCompact)
                        .padding(horizontal = Spacing.Small)
                        .padding(vertical = Spacing.ExtraSmall)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .graphicsLayer {
                            compositingStrategy = if (size.height > Sizes.HeartSize.toPx()) {
                                CompositingStrategy.Offscreen
                            } else {
                                CompositingStrategy.Auto
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            if (size.height > Sizes.HeartSize.toPx()) {
                                val fadeSize = Spacing.Medium.toPx()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startY = 0f, endY = fadeSize
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height - fadeSize, endY = size.height
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        },
                    enabled = canSendMessage,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.chat_type_message),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .size(Sizes.InputButtonCompact),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulseScale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulseAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                }
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = micScale
                            scaleY = micScale
                        }
                        .pointerInput(inputText, canSendMessage) {
                            if (inputText.isNotEmpty() || !canSendMessage) {
                                detectTapGestures(
                                    onTap = {
                                        if (inputText.isNotEmpty()) {
                                            onSendMessage()
                                            dismissedPreviewUrl = null
                                            focusRequester.requestFocus()
                                        }
                                    }
                                )
                            } else {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isSwipeToCancel = false
                                    isArmedForCancel = false
                                    slideOffset = 0f
                                    onMicHeld()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                    var cumulativeDrag = 0f
                                    var cancelTriggered = false

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            if (cancelTriggered || isArmedForCancel) {
                                                isSwipeToCancel = true
                                                onRecordingCancelled()
                                            } else {
                                                onMicReleased()
                                            }
                                            slideOffset = 0f
                                            isArmedForCancel = false
                                            break
                                        }

                                        val dragDelta = change.positionChange().x
                                        cumulativeDrag += dragDelta
                                        slideOffset = cumulativeDrag.coerceAtMost(0f)

                                        val currentDistance = abs(slideOffset)
                                        if (currentDistance >= cancelThresholdPx) {
                                            if (!cancelTriggered) {
                                                cancelTriggered = true
                                                isArmedForCancel = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        } else {
                                            if (cancelTriggered) {
                                                cancelTriggered = false
                                                isArmedForCancel = false
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    shape = CircleShape,
                    color = if (isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (isArmedForCancel) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when {
                            editingMessage != null -> Icons.Default.Check
                            inputText.isNotEmpty() || !canSendMessage -> Icons.Default.ArrowUpward
                            isArmedForCancel -> Icons.Default.Delete
                            else -> Icons.Default.Mic
                        }
                        AnimatedContent(
                            targetState = icon,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)) togetherWith
                                fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
                            },
                            label = "iconAnimation"
                        ) { targetIcon ->
                            Icon(
                                targetIcon,
                                contentDescription = stringResource(if (inputText.isEmpty()) R.string.voice_hold_to_record else R.string.chat_action_send),
                                modifier = Modifier.size(Sizes.IconLarge),
                                tint = if (isArmedForCancel) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                }

            }
        }
        // Hint if restricted
        if (!canSendMessage) {
            Text(
                text = stringResource(R.string.only_admins_can_message_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.ExtraSmall)
            )
        }
    }
}
