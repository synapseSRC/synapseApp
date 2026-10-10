package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.ui.platform.LocalUriHandler

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.media.VoicePlayerManager
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.*
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceAction
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceRecorder
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceRecordingController
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceState
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewViewModel
import com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import java.io.File

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
        onNavigateToChatInfo: (String, String) -> Unit = { _, _ -> },
        onNavigateToProfile: (String) -> Unit = {},
        onNavigateToEvent: (String) -> Unit = {},
        onNavigateToProduct: (String) -> Unit = {},
        viewModel: ChatViewModel = hiltViewModel()
    ) {
    // Initialize the ViewModel with the chat ID
    LaunchedEffect(chatId, participantId) {
        viewModel.initialize(chatId, participantId)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.onVisibilityChanged(true)
                    if (isFirstResume) {
                        isFirstResume = false
                    } else {
                        viewModel.restartSubscriptions()
                        viewModel.refreshMessages()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onVisibilityChanged(false)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onVisibilityChanged(false)
        }
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
    val groupMemberNames by viewModel.groupMemberNames.collectAsState()
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

    val participantAvatarUrl = participantProfile?.avatar
        ?: initialParticipantAvatar?.let {
            if (it.startsWith("http")) it
            else com.synapse.social.studioasinc.shared.core.network.SupabaseClient.constructAvatarUrl(it)
        }

    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var batchGalleryMessage by remember { mutableStateOf<Message?>(null) }
    var batchViewerMessage by remember { mutableStateOf<Message?>(null) }
    var batchViewerInitialIndex by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val cancelThresholdPx = with(density) { 100.dp.toPx() }
    val lockThresholdPx = with(density) { 90.dp.toPx() }
    val directionSlopPx = with(density) { 12.dp.toPx() }

    var selectedMediaFiles by remember { mutableStateOf<List<PickedFile>?>(null) }

    // Voice Recording Infrastructure
    val voiceRecorder = remember { VoiceRecorder(context) }
    val voicePlayerManager = remember { VoicePlayerManager(context.applicationContext) }

    val voiceController = remember {
        VoiceRecordingController(
            scope = coroutineScope,
            voiceRecorder = voiceRecorder,
            voicePlayerManager = voicePlayerManager,
            getOutputFile = { File(context.cacheDir, "temp_voice_${System.currentTimeMillis()}.m4a") },
            onHapticPerform = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            onSendVoiceMessage = { audioFile, durationMs, onResult ->
                viewModel.uploadVoiceMessage(audioFile, durationMs, onResult)
            },
            cancelThresholdPx = cancelThresholdPx,
            lockThresholdPx = lockThresholdPx,
            directionSlopPx = directionSlopPx
        )
    }

    LaunchedEffect(cancelThresholdPx, lockThresholdPx, directionSlopPx) {
        voiceController.updateThresholds(cancelThresholdPx, lockThresholdPx, directionSlopPx)
    }

    val voiceState by voiceController.state.collectAsState()

    DisposableEffect(voiceController) {
        onDispose {
            voiceController.cleanup()
            voicePlayerManager.release()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            voiceController.onPermissionResult(isGranted)
            if (!isGranted) {
                android.widget.Toast.makeText(context, context.getString(R.string.voice_mic_permission_required), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is androidx.compose.foundation.interaction.DragInteraction.Start) {
                focusManager.clearFocus()
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

    LaunchedEffect(error) {
        error?.let { err ->
            if (messages.isNotEmpty()) {
                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    var listReady by remember { mutableStateOf(false) }
    var showDisappearingModeDialog by remember { mutableStateOf(false) }

    var lastScrolledMessageId by remember { mutableStateOf<String?>(null) }
    val newestMessage = remember(messages) { messages.lastOrNull() }
    val newestMessageId = newestMessage?.id

    LaunchedEffect(newestMessageId) {
        if (newestMessageId != null) {
            if (!listReady) {
                listState.scrollToItem(0)
                listReady = true
                lastScrolledMessageId = newestMessageId
            } else if (newestMessageId != lastScrolledMessageId) {
                val isFromMe = newestMessage.senderId == currentUserId
                val isNearBottom = listState.firstVisibleItemIndex <= 3

                if (isFromMe || isNearBottom) {
                    listState.animateScrollToItem(0)
                }
                lastScrolledMessageId = newestMessageId
            }
        }
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
                onNavigateToChatInfo = onNavigateToChatInfo,
                onSetDisappearingMode = { showDisappearingModeDialog = true },
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
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
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
                        groupMemberNames = groupMemberNames,
                        listState = listState,
                        isLoadingMore = isLoadingMore,
                        typingStatus = typingStatus,
                        onLoadMore = { if (hasMoreMessages) viewModel.loadMoreMessages() },
                        onSingleTap = { selectedMessageForMenu = it },
                        onToggleSelection = { viewModel.toggleMessageSelection(it) },
                        onSwipeToReply = { viewModel.setReplyingToMessage(it) },
                        onReactionSelected = { id, reaction -> viewModel.toggleMessageReaction(id, reaction) },
                        onNavigateToProfile = onNavigateToProfile,
                        onLocateMessage = viewModel::loadQuotedMessage,
                        onOpenBatchGallery = { batchGalleryMessage = it },
                        onVoteOption = { messageId, optionId -> viewModel.voteInPoll(messageId, optionId) },
                        onOpenPost = { postId -> try { uriHandler.openUri("synapse://post/$postId") } catch (_: Exception) {} },
                        onOpenStory = { storyId -> try { uriHandler.openUri("synapse://story/$storyId") } catch (_: Exception) {} },
                        onOpenEphemeralMedia = { mediaUrl -> 
                        try { 
                            uriHandler.openUri(mediaUrl)
                            messages.find { it.mediaUrl == mediaUrl && it.metadataContainer?.viewOnce?.isConsumed != true }?.id?.let { viewModel.markViewOnceConsumed(it) }
                        } catch (_: Exception) {} 
                    },
                        onOpenEvent = { eventId -> try { uriHandler.openUri("synapse://event/$eventId") } catch (_: Exception) {} },
                        onOpenProduct = { productId -> try { uriHandler.openUri("synapse://product/$productId") } catch (_: Exception) {} },
                        onOpenMap = { lat, lon -> try { uriHandler.openUri("https://maps.google.com/?q=$lat,$lon") } catch (_: Exception) {} },
                        onContactAction = { phone -> try { uriHandler.openUri("tel:$phone") } catch (_: Exception) {} }
                    )

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

            MessageSummaryDialog(
                isSummarizingMessage = isSummarizingMessage,
                messageSummary = messageSummary,
                onDismissRequest = viewModel::clearMessageSummary
            )

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

            // Redesigned Voice Input Bar & Host
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
                    participantDisplayName = participantProfile?.displayName
                        ?: participantProfile?.name
                        ?: participantProfile?.username
                        ?: initialParticipantName?.takeUnless { isGroupChat },
                    replyingToSenderName = quotedSenderDisplayName(
                        message = replyingToMessage,
                        currentUserId = currentUserId,
                        isGroupChat = isGroupChat,
                        groupMemberNames = groupMemberNames,
                        participantDisplayName = participantProfile?.displayName
                            ?: participantProfile?.name
                            ?: participantProfile?.username,
                        initialParticipantName = initialParticipantName
                    ),
                    fontScale = chatFontScale,
                    getLinkMetadataUseCase = linkPreviewViewModel.getLinkMetadataUseCase,
                    context = context,
                    voiceState = voiceState,
                    onVoiceAction = { action ->
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (action is VoiceAction.StartRecording && !hasPermission) {
                            voiceController.requestPermissionAndRecord()
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            voiceController.handleAction(action)
                        }
                    },
                    onInputTextChange = viewModel::onInputTextChange,
                    onSendMessage = viewModel::sendMessage,
                    onCancelReply = viewModel::cancelReply,
                    onCancelEditing = viewModel::cancelEditing,
                    onUploadAndSendMedia = viewModel::uploadAndSendMedia,
                    onFilesSelected = { files -> selectedMediaFiles = files }
                )
            }

            ChatSummaryDialog(
                chatSummary = chatSummary,
                onDismissRequest = viewModel::clearSummary
            )

            batchGalleryMessage?.let { msg ->
                BatchMediaGalleryDialog(
                    attachments = msg.attachments,
                    caption = msg.content.takeIf { it.isNotBlank() },
                    onDismiss = { batchGalleryMessage = null },
                    onMediaItemClick = { clickedIndex ->
                        batchViewerInitialIndex = clickedIndex
                        batchGalleryMessage = null
                        batchViewerMessage = msg
                    }
                )
            }

            batchViewerMessage?.let { msg ->
                BatchMediaViewerDialog(
                    attachments = msg.attachments,
                    initialIndex = batchViewerInitialIndex,
                    caption = msg.content.takeIf { it.isNotBlank() },
                    onDismiss = { batchViewerMessage = null }
                )
            }
        }

        val disappearingMode by viewModel.disappearingMode.collectAsState()
        if (showDisappearingModeDialog) {
            com.synapse.social.studioasinc.feature.inbox.inbox.components.DisappearingModeDialog(
                currentMode = disappearingMode,
                onModeSelected = { viewModel.setDisappearingMode(it) },
                onDismissRequest = { showDisappearingModeDialog = false }
            )
        }
    }

    selectedMediaFiles?.let { files ->
        MediaPreviewDialog(
            selectedFiles = files,
            context = context,
            onDismissRequest = {
                selectedMediaFiles = null
            },
            onSendMedia = { finalFiles, caption, quality ->
                val validFiles = finalFiles.mapNotNull { pickedFile ->
                    val filePath = com.synapse.social.studioasinc.core.util.FileUtils.validateAndCleanPath(context, pickedFile.uri.toString())
                    if (filePath != null) filePath to pickedFile else null
                }
                if (validFiles.isNotEmpty()) {
                    viewModel.uploadAndSendMultipleMedia(validFiles, caption, quality)
                }
                selectedMediaFiles = null
            }
        )
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface VoiceUploadServiceEntryPoint {
    fun getVoiceUploadService(): VoiceUploadService
}
