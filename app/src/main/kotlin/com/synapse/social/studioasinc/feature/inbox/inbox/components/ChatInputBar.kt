package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.ui.graphics.RectangleShape

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceAction
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceInputHost
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceState
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    voiceState: VoiceState = VoiceState.Normal,
    reducedMotion: Boolean,
    onVoiceAction: (VoiceAction) -> Unit = {},
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEditing: () -> Unit,
    onAttachmentSelected: (ChatAttachmentCategory) -> Unit,
    replyingToSenderName: String? = null,
    fontScale: Float = 1f
) {
    var dismissedPreviewUrl by remember { mutableStateOf<String?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val attachmentInteraction = remember { MutableInteractionSource() }
    val attachmentPressed by attachmentInteraction.collectIsPressedAsState()
    // My design decision (not in M3): subtle press scale on the composer attachment action.
    val attachmentScale by animateFloatAsState(
        targetValue = if (attachmentPressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "chatAttachmentButtonPress"
    )
    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val inputWidthFraction by animateFloatAsState(
        targetValue = if ((isFocused && isImeVisible) || inputText.isNotEmpty()) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "inputWidthFraction"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space4)
            .padding(top = Spacing.space4, bottom = Spacing.space16)
    ) {
        // Shared quote preview keeps the composer context consistent with sent reply bubbles.
        AnimatedVisibility(
            visible = replyingToMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            QuotedMessagePreview(
                originalMessage = replyingToMessage,
                originalMessageId = replyingToMessage?.id,
                currentUserId = currentUserId,
                originalSenderName = replyingToSenderName ?: participantDisplayName,
                fontScale = fontScale,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge),
                trailingContent = {
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(Sizes.IconLarge)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_reply_cancel),
                            modifier = Modifier.size(Sizes.IconSemiMedium)
                        )
                    }
                }
            )
        }

        // Editing Header
        AnimatedVisibility(
            visible = editingMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = if (replyingToMessage == null) RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge) else RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(Sizes.IconSemiMedium))
                    Spacer(modifier = Modifier.width(Spacing.space8))
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
                    .padding(Spacing.space4),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
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
                modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space8).fillMaxWidth()
            )
        }

        // Voice Input Host wrapping normal composer and voice morph overlay
        VoiceInputHost(
            voiceState = voiceState,
            onVoiceAction = onVoiceAction,
            showSendButton = inputText.isNotEmpty(),
            reducedMotionOverride = reducedMotion,
            onSendMessage = {
                onSendMessage()
                dismissedPreviewUrl = null
                focusRequester.requestFocus()
            },
            modifier = Modifier
                .fillMaxWidth(inputWidthFraction)
                .align(Alignment.CenterHorizontally)
        ) {
            // Normal Chat Input Left/Middle Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space4),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier.padding(bottom = (Sizes.SendButton - 38.dp) / 2)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(38.dp).graphicsLayer { scaleX = attachmentScale; scaleY = attachmentScale }
                    ) {
                        IconButton(
                            onClick = { showAttachmentMenu = true },
                            modifier = Modifier.fillMaxSize(),
                            interactionSource = attachmentInteraction
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.chat_attachment_open_action),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (showAttachmentMenu) {
                        ChatAttachmentSheet(
                            onDismissRequest = { showAttachmentMenu = false },
                            onCategorySelected = { category ->
                                showAttachmentMenu = false
                                onAttachmentSelected(category)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.space8))

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Sizes.InputButtonCompact)
                        .padding(horizontal = Spacing.space4)
                        .padding(vertical = Spacing.space4)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        if (!canSendMessage) {
            Text(
                text = stringResource(R.string.only_admins_can_message_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.space4)
            )
        }
    }
}
