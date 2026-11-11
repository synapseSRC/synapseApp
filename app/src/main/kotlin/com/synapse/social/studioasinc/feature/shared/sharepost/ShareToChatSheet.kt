package com.synapse.social.studioasinc.feature.shared.sharepost

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.components.SharedPostCardBubble
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.SharedPostMetadata
import kotlinx.coroutines.delay

/** Dismiss delay after the "Shared to X" confirmation, so the result stays briefly visible. */
private const val CONFIRMATION_DISMISS_DELAY_MS = 1_200L

/**
 * Conversation picker shown when sharing a feed post. Renders the [Conversation] list from
 * [SharePostSheetViewModel], offers the external app chooser as a fallback, and moves to a review
 * step so the post is only sent after the user confirms the recipient.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatSheet(
    viewModel: SharePostSheetViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onShareExternal: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Dismiss shortly after the "Shared to X" confirmation so the send result is visible.
    LaunchedEffect(uiState.confirmation) {
        if (uiState.confirmation != null) {
            delay(CONFIRMATION_DISMISS_DELAY_MS)
            viewModel.onMessageShown()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.onSheetDismissed()
            onDismiss()
        },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        val pendingConversation = uiState.pendingConversation
        if (pendingConversation == null) {
            ConversationPickerContent(
                uiState = uiState,
                onQueryChange = viewModel::onQueryChange,
                onConversationSelected = viewModel::selectConversation,
                onShareExternal = onShareExternal
            )
        } else {
            ShareConfirmationContent(
                preview = uiState.preview,
                conversation = pendingConversation,
                isSending = uiState.sendingChatId == pendingConversation.chatId,
                onConfirm = viewModel::confirmSend,
                onChooseAnother = viewModel::cancelSelection
            )
        }

        uiState.error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space4)
            )
        }
        uiState.confirmation?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Spacing.space16, vertical = Spacing.space4)
            )
        }
    }
}

/** Step 1: search and pick the conversation that should receive the post. */
@Composable
private fun ConversationPickerContent(
    uiState: SharePostSheetUiState,
    onQueryChange: (String) -> Unit,
    onConversationSelected: (Conversation) -> Unit,
    onShareExternal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16)
            .padding(bottom = Spacing.space16)
    ) {
        Text(
            text = stringResource(R.string.share_to_chat_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(Spacing.space12))

        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.share_to_chat_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            }
        )
        Spacer(modifier = Modifier.height(Spacing.space8))

        val filteredConversations = uiState.conversations.filter { conversation ->
            uiState.query.isBlank() ||
                conversation.participantName.contains(uiState.query, ignoreCase = true)
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.HeightPreview),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            filteredConversations.isEmpty() -> {
                Text(
                    text = if (uiState.query.isBlank()) {
                        stringResource(R.string.share_to_chat_empty)
                    } else {
                        stringResource(R.string.share_to_chat_no_results)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.space32)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.HeightPreview),
                    contentPadding = PaddingValues(vertical = Spacing.space4)
                ) {
                    items(filteredConversations, key = { conversation -> conversation.chatId }) { conversation ->
                        ShareConversationRow(
                            conversation = conversation,
                            isSending = uiState.sendingChatId == conversation.chatId,
                            onClick = { onConversationSelected(conversation) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.space8))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShareExternal() }
                .padding(vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Spacing.space12))
            Text(
                text = stringResource(R.string.share_to_external_apps),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Step 2: review the post and the recipient, then confirm or go back to the picker. */
@Composable
private fun ShareConfirmationContent(
    preview: SharedPostMetadata?,
    conversation: Conversation,
    isSending: Boolean,
    onConfirm: () -> Unit,
    onChooseAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16)
            .padding(bottom = Spacing.space16)
    ) {
        Text(
            text = stringResource(R.string.share_to_chat_confirm_title, conversation.participantName),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(Spacing.space4))
        Text(
            text = stringResource(R.string.share_to_chat_confirm_message, conversation.participantName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.space12))
        Text(
            text = stringResource(R.string.share_to_chat_preview_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.space4))
        SharedPostCardBubble(
            metadata = preview,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.space12))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatarUrl = conversation.participantAvatar?.let { avatar ->
                if (avatar.startsWith("http")) avatar
                else com.synapse.social.studioasinc.shared.core.network.SupabaseClient.constructAvatarUrl(avatar)
            }
            UserAvatar(
                avatarUrl = avatarUrl,
                displayName = conversation.participantName,
                size = Sizes.AvatarMedium
            )
            Spacer(modifier = Modifier.width(Spacing.space12))
            Text(
                text = conversation.participantName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(Spacing.space12))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onChooseAnother, enabled = !isSending) {
                Text(stringResource(R.string.share_to_chat_confirm_back))
            }
            Spacer(modifier = Modifier.width(Spacing.space8))
            Button(onClick = onConfirm, enabled = !isSending) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Sizes.IconDefault),
                        strokeWidth = Sizes.BorderDefault
                    )
                } else {
                    Text(stringResource(R.string.share_to_chat_confirm_send))
                }
            }
        }
    }
}

@Composable
private fun ShareConversationRow(
    conversation: Conversation,
    isSending: Boolean,
    onClick: () -> Unit
) {
    val avatarUrl = conversation.participantAvatar?.let { avatar ->
        if (avatar.startsWith("http")) avatar
        else com.synapse.social.studioasinc.shared.core.network.SupabaseClient.constructAvatarUrl(avatar)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSending, onClick = onClick)
            .padding(vertical = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = avatarUrl,
            displayName = conversation.participantName,
            size = Sizes.AvatarMedium
        )
        Spacer(modifier = Modifier.width(Spacing.space12))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.participantName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.lastMessage.isNotBlank()) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.space8))
        if (isSending) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.IconDefault),
                strokeWidth = Sizes.BorderDefault
            )
        } else if (conversation.isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = stringResource(R.string.cd_pinned_conversation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
