package com.synapse.social.studioasinc.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.math.absoluteValue

@Composable
fun DesktopMainScreen(
    viewModel: DesktopChatViewModel = koinInject()
) {
    val conversations by viewModel.conversations.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val isLoadingConversations by viewModel.isLoadingConversations.collectAsState()
    val isLoadingMessages by viewModel.isLoadingMessages.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUserId = remember { viewModel.getCurrentUserId() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0) // Avoid Android-only WindowInsets.systemBars on Desktop
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Master View (Left Sidebar)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()

                    if (isLoadingConversations) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (error != null) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (conversations.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Forum,
                            headline = "No conversations found",
                            description = "Start a new conversation to begin chatting with your friends."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(conversations) { conversation ->
                                ChatListItem(
                                    conversation = conversation,
                                    isSelected = conversation.chatId == selectedConversation?.chatId,
                                    onClick = { viewModel.selectConversation(conversation) }
                                )
                            }
                        }
                    }
                }
            }

            // Detail View (Right Content)
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val conversation = selectedConversation
                if (conversation != null) {
                    ChatDetailView(
                        conversation = conversation,
                        messages = messages,
                        isLoading = isLoadingMessages,
                        currentUserId = currentUserId,
                        error = error,
                        onSendMessage = { text -> viewModel.sendMessage(text) }
                    )
                } else {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        headline = "Select a chat",
                        description = "Choose a conversation from the sidebar list to start messaging."
                    )
                }
            }
        }
    }
}

fun getAvatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFFE57373), // Soft Red
        Color(0xFFF06292), // Soft Pink
        Color(0xFFBA68C8), // Soft Purple
        Color(0xFF9575CD), // Soft Deep Purple
        Color(0xFF7986CB), // Soft Indigo
        Color(0xFF64B5F6), // Soft Blue
        Color(0xFF4FC3F7), // Soft Light Blue
        Color(0xFF4DB6AC), // Soft Teal
        Color(0xFF81C784), // Soft Green
        Color(0xFFFFB74D), // Soft Orange
        Color(0xFFFF8A65)  // Soft Deep Orange
    )
    val index = name.hashCode().absoluteValue % colors.size
    return colors[index]
}

@Composable
fun ContactAvatar(
    name: String,
    avatarUrl: String?,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(modifier = modifier.size(size)) {
        val initials = name.trim().take(1).uppercase()
        val backgroundColor = getAvatarColor(name)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.BottomEnd)
                    .padding(1.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
    }
}

@Composable
fun ChatListItem(conversation: Conversation, isSelected: Boolean, onClick: () -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            name = conversation.participantName,
            avatarUrl = conversation.participantAvatar,
            isOnline = conversation.isOnline
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.participantName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                val relativeTime = TimestampFormatter.formatRelative(conversation.lastMessageTime)
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (conversation.unreadCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailView(
    conversation: Conversation,
    messages: List<Message>,
    isLoading: Boolean,
    currentUserId: String?,
    error: String? = null,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to the newest message (last item) whenever the message list changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(
                    name = conversation.participantName,
                    avatarUrl = conversation.participantAvatar,
                    isOnline = conversation.isOnline
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = conversation.participantName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (conversation.isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conversation.isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Chat History
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            } else if (messages.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ChatBubbleOutline,
                    headline = "No messages yet",
                    description = "This is the beginning of your chat history with ${conversation.participantName}."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    items(messages) { message ->
                        MessageItem(message, currentUserId)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Message Input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.Enter && !it.isShiftPressed) {
                                if (messageText.isNotBlank()) {
                                    onSendMessage(messageText)
                                    messageText = ""
                                }
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, currentUserId: String?) {
    val isMe = currentUserId != null && message.isFromMe(currentUserId)
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 0.dp
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            val formattedTime = remember(message.createdAt) {
                try {
                    val instant = Instant.parse(message.createdAt)
                    val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                    "${localTime.hour.toString().padStart(2, '0')}:${localTime.minute.toString().padStart(2, '0')}"
                } catch (e: Exception) {
                    TimestampFormatter.formatRelative(message.createdAt)
                }
            }
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    headline: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
