package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile
import com.synapse.social.studioasinc.feature.shared.components.picker.SynapseFilePicker


import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView


import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


import com.synapse.social.studioasinc.feature.shared.components.post.ReactionPicker
import com.synapse.social.studioasinc.domain.model.ReactionType as AppReactionType
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.rememberVectorPainter

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import com.synapse.social.studioasinc.feature.shared.components.UserAvatarWithStatus
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R

import androidx.compose.ui.draw.blur
import com.synapse.social.studioasinc.shared.domain.model.settings.WallpaperType
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatThemePreset
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatShimmer
import com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatTopAppBar
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageContextMenu
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageSummaryDialog
import com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatSummaryDialog
import com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatInputBar
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MediaPreviewDialog
import com.synapse.social.studioasinc.feature.inbox.inbox.components.DateDividerChip
import com.synapse.social.studioasinc.feature.inbox.inbox.components.GroupPosition
import com.synapse.social.studioasinc.feature.inbox.inbox.components.MessageBubble
import com.synapse.social.studioasinc.feature.inbox.inbox.components.UnreadDividerRow
import com.synapse.social.studioasinc.feature.inbox.inbox.components.isWithinTimeThreshold
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.feature.shared.theme.StatusRead
import com.synapse.social.studioasinc.feature.shared.theme.LightPrimaryContainer
import com.synapse.social.studioasinc.feature.shared.theme.ForestBubbleBackground
import com.synapse.social.studioasinc.feature.shared.theme.ForestBubbleText
import com.synapse.social.studioasinc.feature.shared.theme.SunsetBubbleBackground
import com.synapse.social.studioasinc.feature.shared.theme.SunsetBubbleText
import com.synapse.social.studioasinc.feature.shared.theme.Gray200
import com.synapse.social.studioasinc.feature.shared.theme.Gray900
import com.synapse.social.studioasinc.feature.shared.theme.DarkPrimaryContainer
import com.synapse.social.studioasinc.feature.shared.theme.LightOnPrimaryContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewCard
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewViewModel
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import androidx.hilt.navigation.compose.hiltViewModel

import com.synapse.social.studioasinc.shared.domain.service.AudioRecorder
import kotlinx.coroutines.delay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceRecorder
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    linkPreviewViewModel: LinkPreviewViewModel = hiltViewModel(),
    chatId: String,
    participantId: String? = null,
    initialParticipantName: String? = null,
    initialParticipantAvatar: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String, String) -> Unit = { _, _ -> },
    onNavigateToUserMoreOptions: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    // Initialize the ViewModel with the chat ID
    LaunchedEffect(chatId) {
        viewModel.initialize(chatId, participantId)
    }

    // Re-fetch messages when screen resumes (catches messages missed while screen was off)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshMessages()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val messages by viewModel.messages.collectAsState()
    val chatItems by viewModel.chatItems.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val participantProfile by viewModel.participantProfile.collectAsState()
    val isParticipantActive by viewModel.isParticipantActive.collectAsState()

    val error by viewModel.error.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()
    val smartReplies by viewModel.smartReplies.collectAsState()
    val chatSummary by viewModel.chatSummary.collectAsState()
    val messageSummary by viewModel.messageSummary.collectAsState()
    val isSummarizingMessage by viewModel.isSummarizingMessage.collectAsState()
    val canSendMessage by viewModel.canSendMessage.collectAsState()
    val selectedMessageIds by viewModel.selectedMessageIds.collectAsState()
    val replyingToMessage by viewModel.replyingToMessage.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isGroupChat by viewModel.isGroupChat.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()

    val currentUserId = viewModel.currentUserId ?: ""

    val chatWallpaperType by viewModel.chatWallpaperType.collectAsState()
    val chatWallpaperValue by viewModel.chatWallpaperValue.collectAsState()
    val chatWallpaperBlur by viewModel.chatWallpaperBlur.collectAsState()
    val chatFontScale by viewModel.chatFontScale.collectAsState()
    val chatThemePreset by viewModel.chatThemePreset.collectAsState()
    val chatMessageCornerRadius by viewModel.chatMessageCornerRadius.collectAsState()
    val chatAvatarDisabled by viewModel.chatAvatarDisabled.collectAsState()

    // Resolve avatar URL: prefer loaded profile, fall back to nav arg (constructing full URL if needed)
    val participantAvatarUrl = participantProfile?.avatar
        ?: initialParticipantAvatar?.let {
            if (it.startsWith("http")) it
            else com.synapse.social.studioasinc.shared.core.network.SupabaseClient.constructAvatarUrl(it)
        }

    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }

    @Suppress("DEPRECATION")
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("") }

    // Voice Recording State
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }
    var recordingAmplitude by remember { mutableStateOf(0) }

    val voiceRecorder = remember { VoiceRecorder(context) }
    // Hilt entry point lookup for VoiceUploadService
    val voiceUploadService = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            VoiceUploadServiceEntryPoint::class.java
        ).getVoiceUploadService()
    }
    val coroutineScope = rememberCoroutineScope()

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                android.widget.Toast.makeText(context, context.getString(R.string.voice_mic_permission_required), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Collection of amplitude and timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startTime = System.currentTimeMillis()
            launch {
                voiceRecorder.amplitudeFlow.collect { amp ->
                    recordingAmplitude = amp
                }
            }
            launch {
                while (isRecording) {
                    recordingDurationMs = System.currentTimeMillis() - startTime
                    delay(100)
                }
            }
        } else {
            recordingDurationMs = 0L
            recordingAmplitude = 0
        }
    }

    val handleFileSelection = { uri: Uri?, type: String, caption: String? ->
        uri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(it) ?: ""
            var fileName = "attachment"
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) fileName = cursor.getString(index)
                }
            }

            val filePath = com.synapse.social.studioasinc.core.util.FileUtils.validateAndCleanPath(context, it.toString())
            if (filePath != null) {
                viewModel.uploadAndSendMedia(
                    filePath = filePath,
                    fileName = fileName,
                    contentType = mimeType,
                    messageType = type,
                    caption = caption
                )
            }
        }
    }


    // Show toast for E2EE errors
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    // Hide list until initial scroll is done to prevent header flicker on first render
    var listReady by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new messages arrive
    var previousMessagesSize by remember { mutableStateOf(0) }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (previousMessagesSize == 0) {
                // Initial load: Snap to bottom instantly, then reveal the list
                listState.scrollToItem(0)
                listReady = true
            } else if (messages.size > previousMessagesSize) {
                // New message arrived: Animate scroll
                listState.animateScrollToItem(0)
            }
        }
        previousMessagesSize = messages.size
    }

    Scaffold(
        topBar = {
            val disappearingMode by viewModel.disappearingMode.collectAsState()
            ChatTopAppBar(
                selectedMessageIds = selectedMessageIds,
                messages = messages,
                participantId = participantId,
                participantProfile = participantProfile,
                initialParticipantName = initialParticipantName,
                initialParticipantAvatar = initialParticipantAvatar,
                typingStatus = typingStatus,
                isParticipantActive = isParticipantActive,
                chatId = chatId,
                disappearingMode = disappearingMode,
                isLocked = viewModel.isChatLocked(),
                onClearSelection = viewModel::clearSelection,
                onDeleteSelectedMessages = viewModel::deleteSelectedMessages,
                onNavigateBack = onNavigateBack,
                onSummarizeChat = viewModel::summarizeChat,
                onNavigateToGroupInfo = onNavigateToGroupInfo,
                onNavigateToUserMoreOptions = onNavigateToUserMoreOptions,
                onSetDisappearingMode = viewModel::setDisappearingMode,
                onLockChat = viewModel::lockCurrentChat,
                onUnlockChat = viewModel::unlockCurrentChat
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding())
                .imePadding()
        ) {

            ChatBackground(
                chatWallpaperType = chatWallpaperType,
                chatWallpaperValue = chatWallpaperValue,
                chatWallpaperBlur = chatWallpaperBlur
            )

            when {
                isLoading && messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ChatShimmer(modifier = Modifier.fillMaxWidth())
                    }
                }
                error != null && messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Something went wrong",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        TextButton(onClick = { viewModel.initialize(chatId, participantId) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                else -> {
                    ChatMessageList(
                        modifier = Modifier.graphicsLayer { alpha = if (listReady || messages.isEmpty()) 1f else 0f },
                        chatItems = chatItems,
                        messages = messages,
                        currentUserId = currentUserId,
                        selectedMessageIds = selectedMessageIds,
                        chatFontScale = chatFontScale,
                        chatMessageCornerRadius = chatMessageCornerRadius,
                        chatThemePreset = chatThemePreset,
                        chatAvatarDisabled = chatAvatarDisabled,
                        participantProfile = participantProfile,
                        initialParticipantName = initialParticipantName,
                        participantAvatarUrl = participantAvatarUrl,
                        participantId = participantId,
                        isGroupChat = isGroupChat,
                        listState = listState,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = { if (hasMoreMessages) viewModel.loadMoreMessages() },
                        onToggleSelection = { viewModel.toggleMessageSelection(it) },
                        onSwipeToReply = { viewModel.setReplyingToMessage(it) },
                        onLongClick = { selectedMessageForMenu = it },
                        onReactionSelected = { id, reaction -> viewModel.toggleMessageReaction(id, reaction) },
                        onShowReactionPicker = { selectedMessageForMenu = it },
                        onNavigateToProfile = onNavigateToProfile
                    )

                    // Context Menu and Reactions for messages
                    if (selectedMessageForMenu != null) {
                        MessageContextMenu(
                            selectedMessage = selectedMessageForMenu,
                            currentUserId = currentUserId,
                            onDismissRequest = { selectedMessageForMenu = null },
                            onReactionSelected = viewModel::toggleMessageReaction,
                            onStartEditing = viewModel::startEditing,
                            onDeleteMessageForMe = viewModel::deleteMessageForMe,
                            onDeleteMessageForEveryone = viewModel::deleteMessage,
                            onSummarizeMessage = viewModel::summarizeMessage
                        )
                    }
                }
            }

            // ── Message Summary Dialog ──
            MessageSummaryDialog(
                isSummarizingMessage = isSummarizingMessage,
                messageSummary = messageSummary,
                onDismissRequest = viewModel::clearMessageSummary
            )

            // ── Bottom fade gradient so messages appear to fade behind the floating input ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(Sizes.HeightMedium)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            // ── Floating Input Bar ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                ChatInputBar(
                    replyingToMessage = replyingToMessage,
                    editingMessage = editingMessage,
                    smartReplies = smartReplies,
                    inputText = inputText,
                    canSendMessage = canSendMessage,
                    currentUserId = currentUserId,
                    participantDisplayName = participantProfile?.displayName,
                    getLinkMetadataUseCase = linkPreviewViewModel.getLinkMetadataUseCase,
                    context = context,
                    onInputTextChange = viewModel::onInputTextChange,
                    onSendMessage = viewModel::sendMessage,
                    onCancelReply = viewModel::cancelReply,
                    onCancelEditing = viewModel::cancelEditing,
                    onUploadAndSendMedia = viewModel::uploadAndSendMedia,
                    isRecording = isRecording,
                    recordingDurationMs = recordingDurationMs,
                    recordingAmplitude = recordingAmplitude,
                    onMicHeld = {
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            val tempFile = File(context.cacheDir, "temp_voice_${System.currentTimeMillis()}.m4a")
                            voiceRecorder.start(tempFile)
                            isRecording = true
                        } else {
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onMicReleased = {
                        if (isRecording) {
                            isRecording = false
                            val outputFile = voiceRecorder.stop()
                            if (outputFile != null && recordingDurationMs > 500) { // minimum 0.5s to prevent accidental taps
                                coroutineScope.launch {
                                    val result = voiceUploadService.upload(outputFile, com.synapse.social.studioasinc.shared.domain.model.StorageConfig())
                                    result.onSuccess { url ->
                                        viewModel.uploadAndSendMedia(
                                            filePath = url,
                                            fileName = "voice_message.m4a",
                                            contentType = "audio/mp4",
                                            messageType = "audio"
                                        )
                                        outputFile.delete()
                                    }
                                }
                            } else {
                                outputFile?.delete()
                            }
                        }
                    },
                    onRecordingCancelled = {
                        if (isRecording) {
                            isRecording = false
                            voiceRecorder.cancel()
                        }
                    }
                )
            }

            // Chat Summary Dialog
            ChatSummaryDialog(
                chatSummary = chatSummary,
                onDismissRequest = viewModel::clearSummary
            )
        }
    }

    selectedMediaUri?.let { uri ->
        MediaPreviewDialog(
            selectedMediaUri = uri,
            selectedMediaType = selectedMediaType,
            context = context,
            onDismissRequest = {
                selectedMediaUri = null
            },
            onSendMedia = { finalUri, type, caption ->
                handleFileSelection(finalUri, type, caption)
                selectedMediaUri = null
            }
        )
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface VoiceUploadServiceEntryPoint {
    fun getVoiceUploadService(): VoiceUploadService
}
