package com.synapse.social.studioasinc.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                        onSendMessage = { text -> viewModel.sendMessage(text) }
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
            Text(text = conversation.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChatDetailView(
    conversation: Conversation,
    messages: List<Message>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true
                ) {
                    items(messages) { message ->
                        MessageItem(message)
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
                    shape = CircleShape
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
fun MessageItem(message: Message) {
    // For simplicity, just rendering content, aligned left
    // Real implementation would align based on sender ID (message.isFromMe(currentUserId))
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message.content,
            modifier = Modifier.padding(12.dp)
        )
    }
}
