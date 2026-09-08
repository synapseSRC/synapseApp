package com.synapse.social.studioasinc.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed class ChatListEntry {
    data class DateHeader(val label: String) : ChatListEntry()
    data class MessageEntry(val message: Message) : ChatListEntry()
}

fun formatMessageDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val today = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val msgDate = local.date
        when {
            msgDate == today -> "Today"
            msgDate == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
            else -> "${msgDate.dayOfMonth} ${msgDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${msgDate.year}"
        }
    } catch (e: Exception) { "" }
}

fun formatMessageTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        "$hour:$minute"
    } catch (e: Exception) { "" }
}

fun buildChatEntries(messages: List<Message>): List<ChatListEntry> {
    if (messages.isEmpty()) return emptyList()
    val entries = mutableListOf<ChatListEntry>()
    for (i in messages.indices) {
        val message = messages[i]
        val dateStr = formatMessageDate(message.createdAt)
        entries.add(ChatListEntry.MessageEntry(message))
        val nextDateStr = if (i + 1 < messages.size) formatMessageDate(messages[i + 1].createdAt) else ""
        if (dateStr != nextDateStr) {
            entries.add(ChatListEntry.DateHeader(dateStr))
        }
    }
    return entries
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMainScreen(
    viewModel: DesktopChatViewModel = koinInject()
) {
    val conversations by viewModel.filteredConversations.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val isLoadingConversations by viewModel.isLoadingConversations.collectAsState()
    val isLoadingMessages by viewModel.isLoadingMessages.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chats",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("New chat")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { /* TODO: implement New chat */ }) {
                                Icon(Icons.Default.Create, contentDescription = "New chat")
                            }
                        }
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("Menu")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { /* TODO: implement Menu */ }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                        }
                    }

                    var localQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = localQuery,
                        onValueChange = { localQuery = it; viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        placeholder = { Text("Search or start a new chat") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = { if (localQuery.isNotEmpty()) IconButton(onClick = { localQuery = ""; viewModel.onSearchQueryChanged("") }) { Icon(Icons.Default.Close, contentDescription = "Clear") } },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeFilter == ConversationFilter.ALL,
                            onClick = { viewModel.setFilter(ConversationFilter.ALL) },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = activeFilter == ConversationFilter.UNREAD,
                            onClick = { viewModel.setFilter(ConversationFilter.UNREAD) },
                            label = { Text("Unread") }
                        )
                        FilterChip(
                            selected = activeFilter == ConversationFilter.FAVOURITES,
                            onClick = { viewModel.setFilter(ConversationFilter.FAVOURITES) },
                            label = { Text("Favourites") }
                        )
                    }

                    HorizontalDivider()

                    if (isLoadingConversations) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (conversations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No conversations found")
                        }
                    } else {
                        LazyColumn {
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
                        onSendMessage = { text -> viewModel.sendMessage(text) },
                        currentUserId = viewModel.currentUserId
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a chat to start messaging",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(conversation: Conversation, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = conversation.participantName, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!conversation.isGroup && conversation.unreadCount == 0 && conversation.lastMessage.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (conversation.isGroup && conversation.lastMessage.isNotEmpty()) {
                    Text(
                        text = "You: ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailView(
    conversation: Conversation,
    messages: List<Message>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    currentUserId: String? = null
) {
    var messageText by remember { mutableStateOf("") }

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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = conversation.participantName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Video call") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { /* TODO: implement Video call */ }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video call")
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Voice call") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { /* TODO: implement Voice call */ }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice call")
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Search in chat") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { /* TODO: implement Search in chat */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search in chat")
                        }
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("More options") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { /* TODO: implement More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                    }
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
            } else if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This is the beginning of your chat history with ${conversation.participantName}.")
                }
            } else {
                val chatEntries = remember(messages) { buildChatEntries(messages) }
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val isAtBottom by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 || !listState.canScrollForward } }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true
                ) {
                    items(chatEntries) { entry ->
                        when (entry) {
                            is ChatListEntry.DateHeader -> {
                                DateSeparator(label = entry.label)
                            }
                            is ChatListEntry.MessageEntry -> {
                                MessageItem(message = entry.message, isMe = entry.message.isFromMe(currentUserId ?: ""))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (!isAtBottom) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to latest")
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
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Attach file") } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { /* TODO: implement Attach file */ }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Attach file")
                    }
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Emoji") } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { /* TODO: implement Emoji */ }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji")
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
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
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (messageText.isBlank()) {
                    IconButton(
                        onClick = { /* TODO: implement Record voice note */ }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Record voice note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText)
                                messageText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun DateSeparator(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageItem(message: Message, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                when {
                    message.isDeleted -> {
                        val deletedText = if (isMe) "🚫 You deleted this message" else "🚫 This message was deleted"
                        Text(
                            text = deletedText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    message.messageType == MessageType.CALL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Voice call",
                                tint = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Voice call • ${message.content}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    message.messageType == MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "📎 [${message.messageType.name.lowercase()} attachment]",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (isMe) {
                        when (message.deliveryStatus) {
                            DeliveryStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                            DeliveryStatus.DELIVERED -> {
                                Box(modifier = Modifier.size(18.dp, 14.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Delivered",
                                        modifier = Modifier.size(14.dp).align(Alignment.CenterStart),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Delivered",
                                        modifier = Modifier.size(14.dp).align(Alignment.CenterEnd),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            DeliveryStatus.READ -> {
                                Box(modifier = Modifier.size(18.dp, 14.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(14.dp).align(Alignment.CenterStart),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(14.dp).align(Alignment.CenterEnd),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
