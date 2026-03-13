package com.synapse.social.studioasinc.feature.inbox.inbox.screens

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
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.ChatShimmer
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.feature.shared.theme.StatusRead
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    participantId: String? = null,
    initialParticipantName: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (String, String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = hiltViewModel()
) {
    // Initialize the ViewModel with the chat ID
    LaunchedEffect(chatId) {
        viewModel.initialize(chatId, participantId)
    }

    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val participantProfile by viewModel.participantProfile.collectAsState()
    val error by viewModel.error.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()
    val smartReplies by viewModel.smartReplies.collectAsState()
    val chatSummary by viewModel.chatSummary.collectAsState()
    val selectedMessageIds by viewModel.selectedMessageIds.collectAsState()
    val replyingToMessage by viewModel.replyingToMessage.collectAsState()
    val currentUserId = viewModel.currentUserId ?: ""

    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }

    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("") }
    var mediaCaption by remember { mutableStateOf("") }

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

    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val type = context.contentResolver.getType(uri ?: return@rememberLauncherForActivityResult) ?: ""
        val messageType = if (type.startsWith("video/")) "video" else "image"
        selectedMediaUri = uri
        selectedMediaType = messageType
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val type = context.contentResolver.getType(uri ?: return@rememberLauncherForActivityResult) ?: ""
        val messageType = if (type.startsWith("audio/")) "audio" else "file"
        selectedMediaUri = uri
        selectedMediaType = messageType
    }


    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            if (selectedMessageIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedMessageIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val selectedContent = messages.filter { it.id in selectedMessageIds }.joinToString("\n") { it.content }
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(selectedContent))
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = { viewModel.deleteSelectedMessages() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Participant avatar
                            AsyncImage(
                                model = participantProfile?.avatar,
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_person),
                                error = painterResource(R.drawable.ic_person)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = participantProfile?.displayName
                                        ?: participantProfile?.username
                                        ?: initialParticipantName
                                        ?: participantId ?: "Chat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusText = when {
                                    typingStatus != null && typingStatus!!.isTyping -> "Typing..."
                                    participantProfile?.status?.name == "ONLINE" -> "Online"
                                    else -> participantProfile?.lastSeen?.let { "Last seen ${formatChatTimestamp(it)}" } ?: "Offline"
                                }
                                val statusColor = when {
                                    typingStatus != null && typingStatus!!.isTyping -> MaterialTheme.colorScheme.primary
                                    participantProfile?.status?.name == "ONLINE" -> StatusOnline
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::summarizeChat) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize Chat")
                        }
                        IconButton(onClick = { onNavigateToGroupInfo(chatId, participantProfile?.displayName ?: initialParticipantName ?: "Group") }) {
                            Icon(Icons.Default.Info, contentDescription = "Group Info")
                        }
                        var showMoreMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            val disappearingMode by viewModel.disappearingMode.collectAsState()
                            DropdownMenuItem(
                                text = { Text("Disappearing Messages (${disappearingMode.name})") },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                onClick = {
                                    // For simplicity, cycle through modes in this exercise
                                    val nextMode = when (disappearingMode) {
                                        DisappearingMode.OFF -> DisappearingMode.TWENTY_FOUR_HOURS
                                        DisappearingMode.TWENTY_FOUR_HOURS -> DisappearingMode.SEVEN_DAYS
                                        DisappearingMode.SEVEN_DAYS -> DisappearingMode.OFF
                                        else -> DisappearingMode.OFF
                                    }
                                    viewModel.setDisappearingMode(nextMode)
                                    showMoreMenu = false
                                }
                            )
                            val isLocked = viewModel.isChatLocked()
                            DropdownMenuItem(
                                text = { Text(if (isLocked) "Unlock Chat" else "Lock Chat") },
                                leadingIcon = { Icon(if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    if (isLocked) viewModel.unlockCurrentChat() else viewModel.lockCurrentChat()
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Mute Notifications") },
                                leadingIcon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                                onClick = { showMoreMenu = false }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = paddingValues.calculateTopPadding())
                .imePadding()
        ) {
            when {
                isLoading && messages.isEmpty() -> {
                    ChatShimmer(modifier = Modifier.fillMaxSize())
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
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.initialize(chatId, participantId) }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    val messagesMap = remember(messages) {
                        messages.associateBy { it.id }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        // Extra bottom padding so last messages aren't hidden behind the floating input
                        contentPadding = PaddingValues(
                            start = Spacing.Medium,
                            end = Spacing.Medium,
                            top = Spacing.Medium,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                        reverseLayout = true
                    ) {
                        val reversedMessages = messages.reversed()
                        itemsIndexed(reversedMessages, key = { index, it -> "${it.id}_${index}" }) { index, message ->
                            val newerMessage = if (index > 0) reversedMessages[index - 1] else null
                            val olderMessage = if (index < reversedMessages.size - 1) reversedMessages[index + 1] else null

                            val hasOlder = olderMessage != null && olderMessage.senderId == message.senderId && isWithinTimeThreshold(olderMessage.createdAt, message.createdAt)
                            val hasNewer = newerMessage != null && newerMessage.senderId == message.senderId && isWithinTimeThreshold(message.createdAt, newerMessage.createdAt)

                            val position = when {
                                !hasOlder && !hasNewer -> GroupPosition.SINGLE
                                !hasOlder && hasNewer -> GroupPosition.FIRST
                                hasOlder && !hasNewer -> GroupPosition.LAST
                                else -> GroupPosition.MIDDLE
                            }

                            val isSelected = message.id in selectedMessageIds

                            MessageBubble(
                                message = message,
                                isFromMe = message.isFromMe(currentUserId),
                                position = position,
                                isSelected = isSelected,
                                onToggleSelection = {
                                    if (selectedMessageIds.isNotEmpty()) {
                                        message.id?.let { viewModel.toggleMessageSelection(it) }
                                    }
                                },
                                onSwipeToReply = { viewModel.setReplyingToMessage(message) },
                                replyToMessage = message.replyToId?.let { replyId ->
                                    messagesMap[replyId]
                                },
                                onLongClick = {
                                    if (selectedMessageIds.isNotEmpty()) {
                                        message.id?.let { viewModel.toggleMessageSelection(it) }
                                    } else {
                                        selectedMessageForMenu = message
                                    }
                                },
                                onReactionSelected = { reaction ->
                                    message.id?.let { viewModel.toggleMessageReaction(it, reaction) }
                                }
                            )
                        }
                    }

                    // Context Menu and Reactions for messages
                    if (selectedMessageForMenu != null) {
                        ModalBottomSheet(
                            onDismissRequest = { selectedMessageForMenu = null }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp)
                            ) {
                                // Reactions Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    AppReactionType.values().forEach { reaction ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                val sharedReaction = SharedReactionType.fromString(reaction.name)
                                                selectedMessageForMenu?.id?.let { viewModel.toggleMessageReaction(it, sharedReaction) }
                                                selectedMessageForMenu = null
                                            }
                                        ) {
                                            Image(
                                                painter = painterResource(id = reaction.iconRes),
                                                contentDescription = reaction.displayName,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }

                                Divider()

                                // Options
                                ListItem(
                                    headlineContent = { Text("Copy") },
                                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        selectedMessageForMenu?.let {
                                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(it.content))
                                        }
                                        selectedMessageForMenu = null
                                    }
                                )
                                val isFromMe = selectedMessageForMenu?.senderId == currentUserId
                                if (isFromMe) {
                                    ListItem(
                                        headlineContent = { Text("Edit") },
                                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        modifier = Modifier.clickable {
                                            selectedMessageForMenu?.let { viewModel.startEditing(it) }
                                            selectedMessageForMenu = null
                                        }
                                    )
                                }
                                ListItem(
                                    headlineContent = { Text("Delete for Me") },
                                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        selectedMessageForMenu?.let { viewModel.deleteMessageForMe(it.id!!) }
                                        selectedMessageForMenu = null
                                    }
                                )
                                if (isFromMe) {
                                    ListItem(
                                        headlineContent = { Text("Delete for Everyone", color = MaterialTheme.colorScheme.error) },
                                        leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        modifier = Modifier.clickable {
                                            selectedMessageForMenu?.let { viewModel.deleteMessage(it.id!!) }
                                            selectedMessageForMenu = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Bottom fade gradient so messages appear to fade behind the floating input ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
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
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Replying Header
                AnimatedVisibility(
                    visible = replyingToMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (replyingToMessage?.senderId == currentUserId) "Replying to yourself" else "Replying to ${participantProfile?.displayName ?: "Them"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = replyingToMessage?.content ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = viewModel::cancelReply, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
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
                        shape = if (replyingToMessage == null) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) else RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Editing message", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = editingMessage?.content ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = viewModel::cancelEditing, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel edit", modifier = Modifier.size(16.dp))
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
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        smartReplies.forEach { reply ->
                            AssistChip(
                                onClick = {
                                    viewModel.onInputTextChange(reply)
                                    viewModel.sendMessage()
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

                // Floating input row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji / Attachment button
                        Box {
                            IconButton(onClick = { showAttachmentMenu = true }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Attach file",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showAttachmentMenu,
                                onDismissRequest = { showAttachmentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Gallery (Image/Video)") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        visualMediaLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Document / File") },
                                    onClick = {
                                        showAttachmentMenu = false
                                        documentLauncher.launch("*/*")
                                    },
                                    leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null) }
                                )
                            }
                        }

                        TextField(
                            value = inputText,
                            onValueChange = viewModel::onInputTextChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.chat_type_message)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        @OptIn(ExperimentalFoundationApi::class)
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .combinedClickable(
                                    onClick = viewModel::sendMessage
                                ),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send
                                Icon(icon, contentDescription = if (editingMessage != null) "Save" else "Send", modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            // Chat Summary Dialog
            if (chatSummary != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearSummary() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat Summary")
                        }
                    },
                    text = {
                        Text(chatSummary ?: "")
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearSummary() }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }

    if (selectedMediaUri != null) {
        Dialog(
            onDismissRequest = {
                selectedMediaUri = null
                mediaCaption = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
                if (result.isSuccessful) {
                    result.uriContent?.let { uri ->
                        selectedMediaUri = uri
                    }
                } else {
                    val exception = result.error
                    android.widget.Toast.makeText(context, context.getString(R.string.error_crop_failed, exception?.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Preview") },
                        navigationIcon = {
                            IconButton(onClick = {
                                selectedMediaUri = null
                                mediaCaption = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Preview")
                            }
                        },
                        actions = {
                            if (selectedMediaType == "image") {
                                IconButton(onClick = {
                                    val uriToCrop = selectedMediaUri
                                    if (uriToCrop != null) {
                                        cropImageLauncher.launch(
                                            CropImageContractOptions(
                                                uri = uriToCrop,
                                                cropImageOptions = CropImageOptions().apply {
                                                    guidelines = CropImageView.Guidelines.ON
                                                    activityTitle = context.getString(R.string.title_edit_image)
                                                    cropMenuCropButtonTitle = context.getString(R.string.action_save)
                                                    showCropOverlay = true
                                                    showProgressBar = true
                                                }
                                            )
                                        )
                                    }
                                }) {
                                    Icon(Icons.Default.Crop, contentDescription = "Crop Image")
                                }
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { previewPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(previewPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedMediaType) {
                            "image" -> {
                                AsyncImage(
                                    model = selectedMediaUri,
                                    contentDescription = "Image Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            "video" -> {
                                VideoPlayerBox(mediaUrl = selectedMediaUri.toString())
                            }
                            "audio" -> {
                                AudioPlayer(mediaUrl = selectedMediaUri.toString(), tintColor = MaterialTheme.colorScheme.primary)
                            }
                            else -> {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = "File",
                                    modifier = Modifier.size(120.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = mediaCaption,
                            onValueChange = { mediaCaption = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a caption...") },
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                handleFileSelection(selectedMediaUri, selectedMediaType, mediaCaption.takeIf { it.isNotBlank() })
                                selectedMediaUri = null
                                mediaCaption = ""
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

enum class GroupPosition {
    SINGLE, FIRST, MIDDLE, LAST
}

private fun isWithinTimeThreshold(timeStr1: String?, timeStr2: String?): Boolean {
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
    onReactionSelected: (SharedReactionType) -> Unit = {}
) {
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer

    // UI logic applied carefully matching sender side for sharpness:
    val shape = if (isFromMe) {
        when (position) {
            GroupPosition.SINGLE -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)
            GroupPosition.FIRST -> RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
            GroupPosition.MIDDLE -> RoundedCornerShape(16.dp, 2.dp, 2.dp, 16.dp)
            GroupPosition.LAST -> RoundedCornerShape(16.dp, 2.dp, 16.dp, 16.dp)
        }
    } else {
        when (position) {
            GroupPosition.SINGLE -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)
            GroupPosition.FIRST -> RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
            GroupPosition.MIDDLE -> RoundedCornerShape(2.dp, 16.dp, 16.dp, 2.dp)
            GroupPosition.LAST -> RoundedCornerShape(2.dp, 16.dp, 16.dp, 16.dp)
        }
    }

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 50.dp.toPx() }



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
                top = if (position == GroupPosition.FIRST || position == GroupPosition.SINGLE) Spacing.Small else 0.dp,
                bottom = 0.dp
            )
            .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
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
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

                if (replyToMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (replyToMessage.senderId == message.senderId) "You" else "Them",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = replyToMessage.content,
                                style = MaterialTheme.typography.bodySmall,
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
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    message.mediaUrl?.let { uriHandler.openUri(it) }
                                }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.VIDEO -> {
                        message.mediaUrl?.let {
                            VideoPlayerBox(mediaUrl = it)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.AUDIO -> {
                        message.mediaUrl?.let {
                            AudioPlayer(mediaUrl = it, tintColor = contentColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.FILE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp).clickable {
                                message.mediaUrl?.let { uriHandler.openUri(it) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = stringResource(R.string.chat_cd_file),
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.content.ifEmpty { stringResource(R.string.chat_media_attachment) },
                                color = contentColor,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    else -> {
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
                            style = androidx.compose.ui.text.TextStyle(
                                color = contentColor,
                                fontSize = 15.sp,
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
                                                    try {
                                                        uriHandler.openUri(annotation.item)
                                                    } catch (e: Exception) {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                                        if (intent.resolveActivity(context.packageManager) != null) {
                                                            context.startActivity(intent)
                                                        }
                                                    }
                                                }
                                        }
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.expiresAt != null) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Disappearing Message",
                            modifier = Modifier.size(10.dp),
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                    }
                    if (message.isEncrypted) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "End-to-End Encrypted",
                            modifier = Modifier.size(10.dp),
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
                                    .size(12.dp)
                                    .clickable { showTooltip = !showTooltip },
                                tint = MaterialTheme.colorScheme.error
                            )
                            if (showTooltip) {
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showTooltip,
                                    onDismissRequest = { showTooltip = false }
                                ) {
                                    Text(
                                        text = message.encryptionFailureReason ?: "Encryption failed",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    if (message.isEdited) {
                        Text(
                            text = "Edited",
                            fontSize = 11.sp,
                            color = contentColor.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Text(
                        text = formatMessageTime(message.createdAt),
                        fontSize = 11.sp,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                    if (isFromMe) {
                        Text(
                            text = when (message.deliveryStatus) {
                                com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.READ -> "✓✓"
                                com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.DELIVERED -> "✓✓"
                                else -> "✓"
                            },
                            fontSize = 11.sp,
                            color = if (message.deliveryStatus == com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.READ) StatusRead
                                    else contentColor.copy(alpha = 0.6f)
                        )
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

/**
 * Formats an ISO timestamp for "last seen" display.
 */
private fun formatChatTimestamp(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun AudioPlayer(mediaUrl: String, tintColor: Color, viewModel: com.synapse.social.studioasinc.feature.shared.reels.VideoPlayerViewModel = hiltViewModel(key = mediaUrl)) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying = uiState.isPlaying
    val durationMs = uiState.duration
    val currentPositionMs = uiState.currentPosition

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mediaUrl) {
        viewModel.initializePlayer(mediaUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    viewModel.pause()
                } else {
                    viewModel.play()
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.chat_action_pause_audio else R.string.chat_action_play_audio),
                tint = tintColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Simple progress bar
        val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = tintColor,
            trackColor = tintColor.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Time display
        val displayTimeMs = if (isPlaying) currentPositionMs else durationMs
        val totalSeconds = displayTimeMs / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        Text(
            text = String.format("%02d:%02d", m, s),
            color = tintColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun VideoPlayerBox(mediaUrl: String, viewModel: com.synapse.social.studioasinc.feature.shared.reels.VideoPlayerViewModel = hiltViewModel(key = mediaUrl)) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mediaUrl) {
        viewModel.initializePlayer(mediaUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val player = viewModel.getPlayerInstance()
        if (player != null) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = true
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
