package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchCategory
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchResult
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchState
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxExpressiveDimens
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InboxConversationRow(
    conversation: Conversation,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
    position: GroupedItemPosition = GroupedItemPosition.Single,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "conversation-row-press"
    )
    val rowShape = position.shape
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else cardSurfaceColor(),
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = rowShape,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            // Clip here, not only on the Surface: the ripple is drawn by this node's indication
            // layer, which sits outside the Surface's own rounded clip.
            .clip(rowShape)
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = InboxExpressiveDimens.ConversationRowHeight)
                .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(InboxExpressiveDimens.ConversationAvatarSize)
                )
            } else {
                UserAvatar(
                    avatarUrl = conversation.participantAvatar,
                    displayName = conversation.participantName,
                    size = InboxExpressiveDimens.ConversationAvatarSize
                )
            }
            Spacer(Modifier.width(Spacing.space12))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.isGroup) {
                        Icon(Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.space16))
                        Spacer(Modifier.width(Spacing.space4))
                    }
                    Text(
                        text = conversation.participantName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val timestamp = conversation.lastMessageTime?.let { TimestampFormatter.formatRelative(it) }.orEmpty()
                    if (timestamp.isNotBlank()) {
                        Spacer(Modifier.width(Spacing.space8))
                        Text(timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(Spacing.space4))
                Text(
                    text = conversation.lastMessage.ifBlank { "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.space8))
            if (trailingContent != null) {
            } else {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                        if (conversation.isPinned) Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.inbox_action_unpin), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.space16))
                        if (conversation.isMuted) Icon(Icons.Rounded.VolumeOff, contentDescription = stringResource(R.string.inbox_action_unmute), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.space16))
                    }
                    if (conversation.unreadCount > 0) {
                        Spacer(Modifier.height(Spacing.space4))
                        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InboxSearchScreen(
    state: InboxSearchState,
    recentConversations: List<Conversation>,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (InboxSearchCategory) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onOpenPerson: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!recognized.isNullOrBlank()) onQueryChange(recognized)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16, vertical = Spacing.space8)
                .height(InboxExpressiveDimens.SearchBarHeight),
            placeholder = { Text(stringResource(R.string.inbox_search_placeholder), maxLines = 1) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.inbox_action_clear_search))
                        }
                    }
                    IconButton(onClick = {
                        try {
                            voiceLauncher.launch(
                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.inbox_search_voice_prompt))
                                }
                            )
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.inbox_search_voice_unavailable, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Rounded.Mic, contentDescription = stringResource(R.string.inbox_search_voice_prompt))
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.space16),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            listOf(
                InboxSearchCategory.UNREAD to R.string.inbox_search_unread,
                InboxSearchCategory.PHOTOS to R.string.inbox_search_photos,
                InboxSearchCategory.LINKS to R.string.inbox_search_links,
                InboxSearchCategory.DOCS to R.string.inbox_search_docs
            ).forEach { (category, label) ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(label)) },
                    leadingIcon = if (state.category == category) {{ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(InboxExpressiveDimens.FilterChipIconSize)) }} else null
                )
            }
        }
        Text(
            text = stringResource(R.string.inbox_search_recent_matches),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.space16, end = Spacing.space16, top = Spacing.space16, bottom = Spacing.space8)
        )
        if (state.query.isBlank()) {
            if (recentConversations.isEmpty()) {
                Text(stringResource(R.string.inbox_search_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8))
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space8), verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    items(recentConversations.take(3), key = { "recent-${it.chatId}" }) { conversation ->
                        InboxConversationRow(conversation = conversation, onClick = { onOpenConversation(conversation) })
                    }
                }
            }
            return
        }
        if (state.failedConversationCount > 0 || state.contactsSearchFailed) {
            Text(stringResource(R.string.inbox_search_partial_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space4))
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(Spacing.space24), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.messages.isEmpty() && state.people.isEmpty()) {
            Text(stringResource(R.string.inbox_search_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space8))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space8),
                verticalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                if (state.messages.isNotEmpty()) {
                    items(state.messages, key = { "message-${it.conversation.chatId}-${it.message.id}" }) { match ->
                        SearchMessageRow(result = match, onClick = { onOpenConversation(match.conversation) })
                    }
                }
                if (state.people.isNotEmpty()) {
                    item(key = "people-header") {
                        Text(stringResource(R.string.inbox_search_people), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.space8))
                    }
                    items(state.people, key = { "person-${it.uid}" }) { person ->
                        SearchPersonRow(person = person, onClick = { onOpenPerson(person) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMessageRow(result: InboxSearchResult, onClick: () -> Unit) {
    val message = result.message
    val typeName = message.messageType.name.lowercase()
    val icon = when {
        typeName.contains("image") || typeName.contains("photo") -> Icons.Rounded.Description
        typeName.contains("file") || typeName.contains("document") -> Icons.Rounded.Description
        typeName.contains("link") || message.linkPreview != null -> Icons.Rounded.Link
        else -> Icons.Rounded.Search
    }
    Surface(
        onClick = onClick,
        color = cardSurfaceColor(),
        shape = RoundedCornerShape(Sizes.CornerLarge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Spacing.space16), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(InboxExpressiveDimens.ConversationAvatarSize)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.width(Spacing.space12))
            Column(Modifier.weight(1f)) {
                Text(result.conversation.participantName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(message.content.ifBlank { message.mediaUrl.orEmpty() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.inbox_message_source_and_time, result.conversation.participantName, TimestampFormatter.formatRelative(message.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchPersonRow(person: User, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = cardSurfaceColor(),
        shape = RoundedCornerShape(Sizes.CornerLarge),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Spacing.space16), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(avatarUrl = person.avatar, displayName = person.displayName ?: person.username, size = InboxExpressiveDimens.ConversationAvatarSize)
            Spacer(Modifier.width(Spacing.space12))
            Column(Modifier.weight(1f)) {
                Text(person.displayName ?: person.username.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val username = person.username
                if (!username.isNullOrBlank()) {
                    Text(stringResource(R.string.inbox_username_format, username), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InboxArchivedScreen(
    conversations: List<Conversation>,
    isLoading: Boolean,
    selectedChatIds: Set<String>,
    onOpenConversation: (Conversation) -> Unit,
    onToggleSelection: (String) -> Unit,
    onUnarchive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionMode = selectedChatIds.isNotEmpty()
    when {
        isLoading && conversations.isEmpty() -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        conversations.isEmpty() -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(Spacing.space24), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.inbox_archive_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space8),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            items(conversations, key = { "archived-${it.chatId}" }) { conversation ->
                InboxConversationRow(
                    conversation = conversation,
                    selected = conversation.chatId in selectedChatIds,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) onToggleSelection(conversation.chatId) else onOpenConversation(conversation)
                    },
                    onLongClick = { onToggleSelection(conversation.chatId) },
                    trailingContent = if (selectionMode) null else {
                        {
                            TextButton(onClick = { onUnarchive(conversation.chatId) }) {
                                Icon(Icons.Rounded.Unarchive, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.inbox_action_unarchive))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InboxSelectionToolbar(
    allPinned: Boolean,
    allMuted: Boolean,
    onPin: () -> Unit,
    onMute: () -> Unit,
    onArchive: () -> Unit,
    onFolder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = CircleShape,
        modifier = modifier.height(InboxExpressiveDimens.FloatingToolbarHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            IconButton(onClick = onPin, modifier = Modifier.size(InboxExpressiveDimens.IconTouchTarget)) {
                Icon(Icons.Rounded.PushPin, contentDescription = stringResource(if (allPinned) R.string.inbox_action_unpin else R.string.inbox_action_pin))
            }
            IconButton(onClick = onMute, modifier = Modifier.size(InboxExpressiveDimens.IconTouchTarget)) {
                Icon(Icons.Rounded.VolumeOff, contentDescription = stringResource(if (allMuted) R.string.inbox_action_unmute else R.string.inbox_action_mute))
            }
            IconButton(onClick = onArchive, modifier = Modifier.size(InboxExpressiveDimens.IconTouchTarget)) {
                Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.inbox_action_archive))
            }
            IconButton(onClick = onFolder, modifier = Modifier.size(InboxExpressiveDimens.IconTouchTarget)) {
                Icon(Icons.Rounded.Folder, contentDescription = stringResource(R.string.inbox_action_folder))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(InboxExpressiveDimens.IconTouchTarget)) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.inbox_action_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
