package com.synapse.social.studioasinc.feature.inbox.inbox.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.components.picker.SynapseFilePicker
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
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
    recordingDurationMs: Long = 0,
    recordingAmplitude: Int = 0,
    onMicHeld: () -> Unit = {},
    onMicReleased: () -> Unit = {},
    onRecordingCancelled: () -> Unit = {}
) {
    var dismissedPreviewUrl by remember { mutableStateOf<String?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val redDotAlpha by rememberInfiniteTransition(label = "red_dot").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_dot_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.ExtraSmall)
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
                shape = if (replyingToMessage == null) RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge) else RoundedCornerShape(0.dp),
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
                    .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                smartReplies.forEach { reply ->
                    AssistChip(
                        onClick = {
                            onInputTextChange(reply)
                            onSendMessage()
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
                modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall).fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = isRecording,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                shape = RoundedCornerShape(Sizes.CornerLarge),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(Sizes.IconSmall).background(Color.Red.copy(alpha = redDotAlpha), CircleShape))
                    Spacer(modifier = Modifier.width(Spacing.Small))

                    val seconds = (recordingDurationMs / 1000) % 60
                    val minutes = (recordingDurationMs / 1000) / 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Simple amplitude indicator
                    val amplitudeRatio = (recordingAmplitude / 32767f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(Spacing.Medium)
                            .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f), RoundedCornerShape(Sizes.CornerSmall))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(amplitudeRatio)
                                .background(MaterialTheme.colorScheme.onErrorContainer, RoundedCornerShape(Sizes.CornerSmall))
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.Medium))
                    Text(
                        text = stringResource(R.string.chat_swipe_cancel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Floating input row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Sizes.CornerMassive),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = Sizes.BorderDefault,
            shadowElevation = Spacing.ExtraSmall
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.None, bottom = Spacing.None, end = Spacing.Tiny),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji / Attachment button
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }, modifier = Modifier.size(Sizes.InputButtonCompact)) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .padding(vertical = Spacing.ExtraSmall),
                    enabled = canSendMessage,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.chat_type_message),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                @OptIn(ExperimentalFoundationApi::class)
                if (inputText.isEmpty() && !isRecording && editingMessage == null) {
                    var dragOffset by remember { mutableStateOf(0f) }
                    Surface(
                        modifier = Modifier
                            .size(Sizes.SendButtonCompact)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    var isCancelled = false
                                    dragOffset = 0f
                                    onMicHeld()

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pan = event.calculatePan()
                                        dragOffset += pan.x

                                        if (dragOffset < -100f && !isCancelled) {
                                            isCancelled = true
                                            onRecordingCancelled()
                                        }

                                        val upOrCancel = event.changes.find { !it.pressed || it.isConsumed }
                                        if (upOrCancel != null) {
                                            if (!isCancelled) {
                                                onMicReleased()
                                            }
                                            break
                                        }
                                    }
                                }
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, contentDescription = "Hold to record voice message", modifier = Modifier.size(Sizes.IconDefault))
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .size(Sizes.SendButtonCompact)
                            .combinedClickable(
                                onClick = { onSendMessage(); dismissedPreviewUrl = null }
                            ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val icon = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send
                            Icon(icon, contentDescription = if (editingMessage != null) "Save" else "Send", modifier = Modifier.size(Sizes.IconDefault))
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.ExtraSmall)
            )
        }
    }
}
