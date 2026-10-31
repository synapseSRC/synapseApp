package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.ui.platform.LocalUriHandler
import android.widget.Toast
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
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
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.call.CallLaunchRequest
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.*
import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceAction
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceRecorder
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceRecordingController
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceRecordingAmbientGlow
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceState
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceGestureConfig
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import com.synapse.social.studioasinc.feature.shared.components.LinkPreviewViewModel
import com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen(
        linkPreviewViewModel: LinkPreviewViewModel = hiltViewModel(),
        chatId: String,
        participantId: String? = null,
        initialParticipantName: String? = null,
        initialParticipantAvatar: String? = null,
        initialMessageId: String? = null,
        onNavigateBack: () -> Unit,
        onNavigateToGroupInfo: (String, String) -> Unit = { _, _ -> },
        onNavigateToChatInfo: (String, String) -> Unit = { _, _ -> },
        onNavigateToProfile: (String) -> Unit = {},
        onNavigateToEvent: (String) -> Unit = {},
        onNavigateToProduct: (String) -> Unit = {},
        onNavigateToSharedContent: (String, Int) -> Unit = { _, _ -> },
        onNavigateToChatSettings: (String) -> Unit = {},
        onStartCall: (CallLaunchRequest) -> Unit = {},
        viewModel: ChatViewModel = hiltViewModel()
    ) {
    val reducedMotion = AnimationUtil.rememberReducedMotion()
    var showEncryptionSheet by rememberSaveable(chatId) { mutableStateOf(false) }
    var showReportDialog by rememberSaveable(chatId) { mutableStateOf(false) }
    var showBlockDialog by rememberSaveable(chatId) { mutableStateOf(false) }
    var showClearChatDialog by rememberSaveable(chatId) { mutableStateOf(false) }

    // Initialize the ViewModel with the chat ID
    LaunchedEffect(chatId, participantId) {
        viewModel.initialize(chatId, participantId)
    }

    val messages by viewModel.messages.collectAsState()
    val chatItems by viewModel.chatItems.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val isScreenVisible by viewModel.isScreenVisible.collectAsState()
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
    val groupTitle by viewModel.groupTitle.collectAsState()
    val groupAvatarUrl by viewModel.groupAvatarUrl.collectAsState()
    val chatDisplayName = if (isGroupChat) {
        groupTitle ?: initialParticipantName
    } else {
        participantProfile?.displayName ?: participantProfile?.username ?: initialParticipantName
    }
    val resolvedChatId by viewModel.resolvedChatId.collectAsState()
    val groupMemberNames by viewModel.groupMemberNames.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()

    var isSearchActive by rememberSaveable(chatId) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(chatId) { mutableStateOf("") }
    var searchJumpMessageId by rememberSaveable(chatId) { mutableStateOf<String?>(null) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val currentUserIdState by viewModel.currentUserId.collectAsState()
val currentUserId = currentUserIdState ?: ""

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

    val launchCall: (Boolean) -> Unit = { isVideo ->
        val peerId = participantId
        if (peerId != null) {
            onStartCall(
                CallLaunchRequest(
                    participantId = peerId,
                    participantName = participantProfile?.displayName?.takeIf { it.isNotBlank() }
                        ?: participantProfile?.name?.takeIf { it.isNotBlank() }
                        ?: participantProfile?.username?.takeIf { it.isNotBlank() }
                        ?: initialParticipantName?.takeIf { it.isNotBlank() }
                        ?: peerId,
                    participantAvatarUrl = participantAvatarUrl,
                    chatId = resolvedChatId?.takeIf { it.isNotBlank() && it != "new" },
                    isVideo = isVideo
                )
            )
        }
    }

    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var batchGalleryMessage by remember { mutableStateOf<Message?>(null) }
    var batchViewerMessage by remember { mutableStateOf<Message?>(null) }
    var batchViewerInitialIndex by remember { mutableIntStateOf(0) }
    var showDeleteMessageDialog by remember { mutableStateOf<Message?>(null) }
    var showDeleteSelectedDialog by rememberSaveable(chatId) { mutableStateOf(false) }

    LaunchedEffect(selectedMessageIds) {
        if (selectedMessageIds.isEmpty()) showDeleteSelectedDialog = false
    }

    val listState = rememberSaveable(chatId, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    var composerHeightPx by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val closeMessageSearch: () -> Unit = {
        isSearchActive = false
        searchQuery = ""
        viewModel.clearMessageSearch()
        focusManager.clearFocus()
    }

    LaunchedEffect(isSearchActive, searchQuery) {
        if (!isSearchActive) return@LaunchedEffect
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            viewModel.clearMessageSearch()
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MILLIS)
        viewModel.searchMessages(chatId, query)
    }

    BackHandler(enabled = isSearchActive, onBack = closeMessageSearch)

    val gestureConfig = remember { VoiceGestureConfig() }
    val cancelThresholdPx = with(density) { gestureConfig.cancelThresholdDp.dp.toPx() }
    val lockThresholdPx = with(density) { gestureConfig.lockThresholdDp.dp.toPx() }
    val directionSlopPx = with(density) { gestureConfig.directionSlopDp.dp.toPx() }
    val composerHeight = with(density) { composerHeightPx.toDp() }
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
            onSoundPerform = { viewModel.playRecordingCue() },
            onSendVoiceMessage = { audioFile, durationMs, onResult ->
                viewModel.uploadVoiceMessage(audioFile, durationMs, onResult)
            },
            gestureConfig = gestureConfig,
            cancelThresholdPx = cancelThresholdPx,
            lockThresholdPx = lockThresholdPx,
            directionSlopPx = directionSlopPx
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, voiceController) {
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
                    voiceController.onPauseBackground()
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

    val messageListBottomPadding = if (composerHeight > 0.dp) {
        composerHeight + Spacing.space12
    } else {
        Sizes.Height100
    }

    var selectedMediaFiles by remember { mutableStateOf<List<PickedFile>?>(null) }
    fun acceptSelectedUris(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                    // Some system photo providers provide a temporary grant instead.
                }
            }
            coroutineScope.launch {
                val results = withContext(Dispatchers.IO) {
                    uris.map { uri -> runCatching { context.toPickedFile(uri) } }
                }
                val files = results.mapNotNull { it.getOrNull() }
                if (files.size != uris.size) {
                    android.widget.Toast.makeText(context, R.string.chat_attachment_read_failed, android.widget.Toast.LENGTH_SHORT).show()
                }
                if (files.isNotEmpty()) selectedMediaFiles = files
            }
        }
    }

    val photoVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { uris -> acceptSelectedUris(uris) }
    )
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { acceptSelectedUris(listOf(it)) } }
    )
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { uri ->
            uri?.let { contactUri ->
                coroutineScope.launch {
                    try {
                        val contactFile = withContext(Dispatchers.IO) { context.toContactVCard(contactUri) }
                        selectedMediaFiles = listOf(contactFile)
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, R.string.chat_attachment_contact_failed, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

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

    val hasInitialTarget = !initialMessageId.isNullOrBlank()
    var initialScrollCompleted by rememberSaveable(chatId) { mutableStateOf(hasInitialTarget) }
    var showDisappearingModeDialog by rememberSaveable(chatId) { mutableStateOf(false) }

    var isUserDragging by remember { mutableStateOf(false) }
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> isUserDragging = true
                is DragInteraction.Stop,
                is DragInteraction.Cancel -> isUserDragging = false
            }
        }
    }

    var lastScrolledMessageClientId by rememberSaveable(chatId) { mutableStateOf<String?>(null) }
    val newestMessage = remember(chatItems) {
        chatItems.filterIsInstance<ChatListItem.MessageItem>().lastOrNull()?.message
    }
    val newestMessageClientId = newestMessage?.clientId

    LaunchedEffect(chatId, newestMessageClientId) {
        if (newestMessageClientId != null) {
            if (!initialScrollCompleted) {
                listState.scrollToItem(0)
                initialScrollCompleted = true
                lastScrolledMessageClientId = newestMessageClientId
            } else if (newestMessageClientId != lastScrolledMessageClientId) {
                val isFromMe = newestMessage.senderId == currentUserId
                val bottomThreshold = if (typingStatus?.isTyping == true) 4 else 3
                val isNearBottom = listState.firstVisibleItemIndex <= bottomThreshold

                if (isFromMe) {
                    listState.scrollToItem(0)
                    lastScrolledMessageClientId = newestMessageClientId
                } else if (isNearBottom) {
                    if (!isUserDragging) {
                        listState.scrollToItem(0)
                        lastScrolledMessageClientId = newestMessageClientId
                    }
                } else {
                    lastScrolledMessageClientId = newestMessageClientId
                }
            }
        }
    }

    LaunchedEffect(isUserDragging) {
        if (!isUserDragging && initialScrollCompleted && newestMessageClientId != null && newestMessageClientId != lastScrolledMessageClientId) {
            val bottomThreshold = if (typingStatus?.isTyping == true) 4 else 3
            if (listState.firstVisibleItemIndex <= bottomThreshold) {
                listState.scrollToItem(0)
                lastScrolledMessageClientId = newestMessageClientId
            }
        }
    }

    val isTyping = typingStatus?.isTyping == true
    LaunchedEffect(isTyping) {
        if (isTyping && initialScrollCompleted && !isUserDragging) {
            val bottomThreshold = 4
            if (listState.firstVisibleItemIndex <= bottomThreshold) {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                ChatSearchTopAppBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onCloseSearch = closeMessageSearch
                )
            } else {
                val disappearingMode by viewModel.disappearingMode.collectAsState()
                val isMuted by viewModel.isMuted.collectAsState()
                val isMuteUpdating by viewModel.isMuteUpdating.collectAsState()
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
                    isGroupChat = isGroupChat,
                    groupTitle = groupTitle,
                    groupAvatarUrl = groupAvatarUrl,
                    groupMemberCount = groupMemberNames.size,
                    isMuted = isMuted,
                    isMuteUpdating = isMuteUpdating,
                    onClearSelection = viewModel::clearSelection,
                    onDeleteSelectedMessages = { showDeleteSelectedDialog = true },
                    onNavigateBack = onNavigateBack,
                    onSummarizeChat = viewModel::summarizeChat,
                    onNavigateToGroupInfo = onNavigateToGroupInfo,
                    onNavigateToChatInfo = onNavigateToChatInfo,
                    onOpenDisappearingMessages = { showDisappearingModeDialog = true },
                    onStartVoiceCall = { launchCall(false) },
                    onStartVideoCall = { launchCall(true) },
                    showCallActions = participantId != null && !isGroupChat,
                    onSearchInChat = {
                        searchJumpMessageId = null
                        isSearchActive = true
                    },
                    onOpenMediaLinksAndDocs = { onNavigateToSharedContent(chatId, 0) },
                    onToggleMuteNotifications = { viewModel.toggleMuteNotifications() },
                    onOpenChatTheme = { onNavigateToChatSettings(chatId) },
                    onReportChat = { showReportDialog = true },
                    onBlockChat = {
                        if (participantId != null) showBlockDialog = true
                    },
                    onClearChat = { showClearChatDialog = true },
                    onExportChat = {
                        viewModel.exportChat(
                            onSuccess = { formattedText ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, formattedText)
                                    putExtra(Intent.EXTRA_SUBJECT, chatDisplayName.orEmpty())
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.chat_menu_export_chat)
                                    )
                                )
                            },
                            onError = {
                                Toast.makeText(
                                    context,
                                    R.string.chat_info_error_export_chat,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .pointerInput(isSearchActive) {
                    detectTapGestures(onTap = {
                        if (!isSearchActive) focusManager.clearFocus()
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
                        Spacer(modifier = Modifier.height(Spacing.space8))
                        TextButton(onClick = { viewModel.initialize(chatId, participantId) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                else -> {
                    ChatMessageList(
                        chatId = chatId,
                        modifier = Modifier.graphicsLayer { alpha = if (initialScrollCompleted || messages.isEmpty()) 1f else 0f },
                        chatItems = chatItems,
                        messages = messages,
                        bottomContentPadding = messageListBottomPadding,
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
                        initialMessageId = searchJumpMessageId ?: initialMessageId,
                        isListVisible = initialScrollCompleted || messages.isEmpty(),
                        isScreenVisible = isScreenVisible,
                        isLoadingMore = isLoadingMore,
typingStatus = typingStatus,
                        typingStatusDisplayName = if (isGroupChat) {
                            typingStatus?.userId?.let { groupMemberNames[it] }
                        } else null,
                        onLoadMore = { if (hasMoreMessages) viewModel.loadMoreMessages() },
                        onSingleTap = { selectedMessageForMenu = it },
                        onToggleSelection = { viewModel.toggleMessageSelection(it) },
                        onSwipeToReply = { viewModel.setReplyingToMessage(it) },
                        onReactionSelected = { id, reaction -> viewModel.toggleMessageReaction(id, reaction) },
                        onNavigateToProfile = onNavigateToProfile,
                        onOpenEncryption = { showEncryptionSheet = true },
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
                        onContactAction = { phone -> try { uriHandler.openUri("tel:$phone") } catch (_: Exception) {} },
                        onRetryClick = viewModel::retrySendMessage
                    )

                    if (selectedMessageForMenu != null) {
                        MessageContextMenu(
                            selectedMessage = selectedMessageForMenu,
                            currentUserId = currentUserId,
                            onDismissRequest = { selectedMessageForMenu = null },
                            onReactionSelected = viewModel::toggleMessageReaction,
                            onStartEditing = viewModel::startEditing,
                            onDeleteMessage = { message ->
                                selectedMessageForMenu = null
                                showDeleteMessageDialog = message
                            },
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

            DeleteMessageDialog(
                message = showDeleteMessageDialog,
                canDeleteForEveryone = showDeleteMessageDialog?.senderId == currentUserId,
                onDismissRequest = { showDeleteMessageDialog = null },
                onDeleteForMe = { message ->
                    showDeleteMessageDialog = null
                    viewModel.deleteMessageForMe(message)
                },
                onDeleteForEveryone = { message ->
                    showDeleteMessageDialog = null
                    viewModel.deleteMessage(message)
                }
            )

            if (showDeleteSelectedDialog) {
                val selectedMessages = messages.filter { it.id in selectedMessageIds }
                DeleteSelectedMessagesDialog(
                    messageCount = selectedMessageIds.size,
                    canDeleteForEveryone = selectedMessages.isNotEmpty() &&
                        selectedMessages.all { it.senderId == currentUserId },
                    onDismissRequest = { showDeleteSelectedDialog = false },
                    onDeleteForMe = {
                        showDeleteSelectedDialog = false
                        viewModel.deleteSelectedMessagesForMe()
                    },
                    onDeleteForEveryone = {
                        showDeleteSelectedDialog = false
                        viewModel.deleteSelectedMessagesForEveryone()
                    }
                )
            }

            if (!isSearchActive) {
                if (voiceState is VoiceState.Recording || voiceState is VoiceState.Locked) {
                    VoiceRecordingAmbientGlow(
                        voiceState = voiceState,
                        reducedMotionOverride = reducedMotion,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            // Project design decision (not an M3 token): keep spill local to the composer and nearby bubbles.
                            .height(184.dp)
                    )
                }

                // Redesigned Voice Input Bar & Host
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { composerHeightPx = it.height }
                ) {
                    ChatInputBar(
                        replyingToMessage = replyingToMessage,
                        editingMessage = editingMessage,
                        smartReplies = smartReplies,
                        reducedMotion = reducedMotion,
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
                        onAttachmentSelected = { category ->
                            focusManager.clearFocus()
                            when (category) {
                                ChatAttachmentCategory.PHOTOS_AND_VIDEOS -> photoVideoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                                ChatAttachmentCategory.DOCUMENTS -> documentPickerLauncher.launch(DOCUMENT_ATTACHMENT_MIME_TYPES)
                                ChatAttachmentCategory.AUDIO -> documentPickerLauncher.launch(arrayOf("audio/*"))
                                ChatAttachmentCategory.FILES -> documentPickerLauncher.launch(arrayOf("*/*"))
                                ChatAttachmentCategory.CONTACTS -> contactPickerLauncher.launch(null)
                            }
                        }
                    )
                }
            }

            if (isSearchActive) {
                ChatSearchResults(
                    query = searchQuery,
                    isSearching = isSearching,
                    results = searchResults,
                    currentUserId = currentUserId,
                    onResultClick = { message ->
                        closeMessageSearch()
                        searchJumpMessageId = message.id
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    senderName = participantProfile?.displayName
                        ?: participantProfile?.name
                        ?: participantProfile?.username
                        ?: initialParticipantName,
                    senderAvatarUrl = participantAvatarUrl
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

        if (showReportDialog) {
            ChatReportDialog(
                isGroupChat = isGroupChat,
                targetName = chatDisplayName,
                onDismiss = { showReportDialog = false },
                onReport = { reason ->
                    viewModel.reportCurrentChat(
                        participantUserId = participantId,
                        reason = reason,
                        onResult = { result ->
                            Toast.makeText(
                                context,
                                if (result.isSuccess) R.string.chat_info_msg_reported else R.string.chat_report_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            )
        }

        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text(stringResource(R.string.chat_menu_block)) },
                text = {
                    Text(
                        stringResource(
                            R.string.chat_info_block_user_confirm,
                            chatDisplayName ?: stringResource(R.string.user)
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBlockDialog = false
                            viewModel.blockCurrentChatParticipant(participantId.orEmpty()) { result ->
                                Toast.makeText(
                                    context,
                                    if (result.isSuccess) R.string.chat_info_msg_blocked else R.string.chat_info_error_block,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showClearChatDialog) {
            AlertDialog(
                onDismissRequest = { showClearChatDialog = false },
                title = { Text(stringResource(R.string.chat_menu_clear_chat)) },
                text = { Text(stringResource(R.string.chat_info_clear_chat_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearChatDialog = false
                            viewModel.clearCurrentChat { result ->
                                Toast.makeText(
                                    context,
                                    if (result.isSuccess) R.string.chat_info_msg_cleared else R.string.chat_info_error_clear_chat,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
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
                if (validFiles.size != finalFiles.size) {
                    android.widget.Toast.makeText(context, R.string.chat_attachment_read_failed, android.widget.Toast.LENGTH_SHORT).show()
                }
                if (validFiles.isNotEmpty()) {
                    val allVisualMedia = validFiles.all { (_, pickedFile) ->
                        pickedFile.mimeType.startsWith("image/") || pickedFile.mimeType.startsWith("video/")
                    }
                    if (allVisualMedia) {
                        viewModel.uploadAndSendMultipleMedia(validFiles, caption, quality)
                    } else {
                        validFiles.forEach { (filePath, pickedFile) ->
                            val messageType = if (pickedFile.mimeType.startsWith("audio/")) "audio" else "file"
                            viewModel.uploadAndSendMedia(
                                filePath = filePath,
                                fileName = pickedFile.fileName,
                                contentType = pickedFile.mimeType,
                                messageType = messageType,
                                caption = caption,
                                quality = quality
                            )
                        }
                    }
                }
                selectedMediaFiles = null
            }
        )
    }
    if (showEncryptionSheet && !isGroupChat) {
        ChatEncryptionSheet(
            chatId = resolvedChatId ?: chatId,
            participantName = participantProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: participantProfile?.name?.takeIf { it.isNotBlank() }
                ?: participantProfile?.username?.takeIf { it.isNotBlank() }
                ?: initialParticipantName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.chat_security_contact_fallback),
            onDismiss = { showEncryptionSheet = false }
        )
    }
}

private const val SEARCH_DEBOUNCE_MILLIS = 300L

private val DOCUMENT_ATTACHMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.oasis.opendocument.spreadsheet",
    "application/vnd.oasis.opendocument.presentation",
    "application/rtf",
    "text/csv",
    "text/plain"
)

private fun Context.toPickedFile(uri: Uri): PickedFile {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    var fileName = getString(R.string.chat_attachment_default_file_name)
    var fileSize = 0L
    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let { fileName = it }
            }
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) fileSize = cursor.getLong(sizeIndex).coerceAtLeast(0L)
        }
    }
    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
    return PickedFile(uri = uri, mimeType = mimeType, fileName = fileName, size = fileSize)
}

private fun Context.toContactVCard(contactUri: Uri): PickedFile {
    val contact = contentResolver.query(
        contactUri,
        arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val lookupIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (lookupIndex >= 0 && !cursor.isNull(lookupIndex)) {
                cursor.getString(lookupIndex) to if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else ""
            } else null
        } else null
    } ?: throw IOException(getString(R.string.chat_attachment_contact_failed))

    val lookupKey = contact.first.takeIf { it.isNotBlank() }
        ?: throw IOException(getString(R.string.chat_attachment_contact_failed))
    val outputFile = File.createTempFile("synapse-contact-", ".vcf", cacheDir)
    try {
        val vCardUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
        val input = contentResolver.openInputStream(vCardUri)
            ?: throw IOException(getString(R.string.chat_attachment_contact_failed))
        input.use { source -> outputFile.outputStream().use { destination -> source.copyTo(destination) } }
        if (outputFile.length() == 0L) throw IOException(getString(R.string.chat_attachment_contact_failed))
    } catch (exception: Exception) {
        outputFile.delete()
        throw exception
    }

    val safeName = contact.second.orEmpty().trim().replace("/", "_").take(80)
        .ifBlank { getString(R.string.chat_attachment_default_file_name) }
    return PickedFile(
        uri = Uri.fromFile(outputFile),
        mimeType = "text/vcard",
        fileName = safeName + ".vcf",
        size = outputFile.length()
    )
}
