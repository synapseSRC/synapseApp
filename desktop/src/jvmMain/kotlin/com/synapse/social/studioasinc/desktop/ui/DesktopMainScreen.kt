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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ChatItem(val id: String, val name: String, val lastMessage: String)

@Composable
fun DesktopMainScreen() {
    val chats = remember {
        listOf(
            ChatItem("1", "Alice", "Hey, how are you?"),
            ChatItem("2", "Bob", "See you tomorrow!"),
            ChatItem("3", "Charlie", "Got the documents."),
            ChatItem("4", "Diana", "Sounds good to me.")
        )
    }

    var selectedChat by remember { mutableStateOf<ChatItem?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
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
                LazyColumn {
                    items(chats) { chat ->
                        ChatListItem(
                            chat = chat,
                            isSelected = chat == selectedChat,
                            onClick = { selectedChat = chat }
                        )
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
            if (selectedChat != null) {
                ChatDetailView(chat = selectedChat!!)
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

@Composable
fun ChatListItem(chat: ChatItem, isSelected: Boolean, onClick: () -> Unit) {
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
            Text(text = chat.name, fontWeight = FontWeight.Bold)
            Text(text = chat.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChatDetailView(chat: ChatItem) {
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
                Text(text = chat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        // Chat History Placeholder
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("This is the beginning of your chat history with ${chat.name}.")
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
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { messageText = "" },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
