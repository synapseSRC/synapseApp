package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatViewModel
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    participantId: String? = null,
    onNavigateBack: () -> Unit,
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
    val currentUserId = viewModel.currentUserId ?: ""

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Participant avatar
                        AsyncImage(
                            model = participantProfile?.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = participantProfile?.displayName
                                    ?: participantProfile?.username
                                    ?: participantId ?: "Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = when (participantProfile?.status?.name) {
                                "ONLINE" -> "Online"
                                else -> participantProfile?.lastSeen?.let { "Last seen ${formatChatTimestamp(it)}" } ?: "Offline"
                            }
                            val statusColor = when (participantProfile?.status?.name) {
                                "ONLINE" -> Color(0xFF4CAF50)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.imePadding() // Key for keyboard behavior
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = viewModel::onInputTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    @OptIn(ExperimentalFoundationApi::class)
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(
                                onClick = viewModel::sendMessage,
                                onLongClick = viewModel::receiveMockMessage
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && messages.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        val reversedMessages = messages.reversed()
                        itemsIndexed(reversedMessages, key = { _, it -> it.id ?: it.hashCode() }) { index, message ->
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

                            MessageBubble(
                                message = message,
                                isFromMe = message.isFromMe(currentUserId),
                                position = position
                            )
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

@Composable
fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    position: GroupPosition = GroupPosition.SINGLE
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

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

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

                val uriHandler = LocalUriHandler.current
                val context = LocalContext.current

                ClickableText(
                    text = annotatedString,
                    style = androidx.compose.ui.text.TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                    ),
                    onClick = { offset ->
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
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                            color = if (message.deliveryStatus == com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus.READ) Color(0xFF4FC3F7)
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
