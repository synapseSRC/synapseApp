package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.inbox.inbox.WordEffectsViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.WordEffectOverlay
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.EmojiEffectCatalog
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.MessageEffect
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.WordEffect
import androidx.compose.runtime.collectAsState

/**
 * Word effects for one conversation.
 *
 * This screen is the only place effects are configured, and it is reached from Chat info, so the
 * setting sits next to the other per-conversation choices instead of in the composer. Everything it
 * changes is stored on the backend through the use cases, which is what makes the effect appear for
 * every participant rather than only on this device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordEffectsScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: WordEffectsViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId) { viewModel.loadEffects(chatId) }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val reducedMotion = AnimationUtil.rememberReducedMotion()

    var isEditorOpen by remember { mutableStateOf(false) }
    var editorTarget by remember { mutableStateOf<WordEffect?>(null) }
    var pendingDelete by remember { mutableStateOf<WordEffect?>(null) }
    var previewText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessageRes) {
        uiState.errorMessageRes?.let { messageRes ->
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    // The editor stays open while the write is in flight, so a rejected trigger keeps the text the
    // user typed; it closes only once the backend has stored the effect.
    LaunchedEffect(uiState.savedCount) {
        if (uiState.savedCount > 0) {
            isEditorOpen = false
            editorTarget = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.word_effects_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {
            IntroCard()
            PreviewCard(
                previewText = previewText,
                onPreviewTextChange = { text ->
                    previewText = text
                    viewModel.updatePreviewText(text)
                },
                previewEffects = uiState.previewEffects,
                reducedMotion = reducedMotion
            )
            EffectsSection(
                isLoading = uiState.isLoading,
                canRetry = uiState.canRetry,
                effects = uiState.effects,
                onRetry = { viewModel.retryLoad(chatId) },
                onToggle = { effect, enabled -> viewModel.setEffectEnabled(effect, enabled) },
                onEdit = { effect ->
                    editorTarget = effect
                    isEditorOpen = true
                },
                onDelete = { effect -> pendingDelete = effect },
                onAdd = {
                    editorTarget = null
                    isEditorOpen = true
                }
            )
        }
    }

    if (isEditorOpen) {
        WordEffectEditorDialog(
            effect = editorTarget,
            isSaving = uiState.isSaving,
            errorMessageRes = uiState.editorErrorRes,
            onDismiss = {
                isEditorOpen = false
                editorTarget = null
                viewModel.clearEditorError()
            },
            onSave = { trigger, emoji, animation ->
                val target = editorTarget
                if (target == null) {
                    viewModel.addEffect(chatId, trigger, emoji, animation)
                } else {
                    viewModel.updateEffect(chatId, target, trigger, emoji, animation)
                }
            }
        )
    }

    pendingDelete?.let { effect ->
        DeleteEffectDialog(
            effect = effect,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                viewModel.deleteEffect(chatId, effect.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun IntroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            Text(
                text = stringResource(R.string.word_effects_intro_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.word_effects_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreviewCard(
    previewText: String,
    onPreviewTextChange: (String) -> Unit,
    previewEffects: List<MessageEffect>,
    reducedMotion: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            Text(
                text = stringResource(R.string.word_effects_preview_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = previewText,
                onValueChange = onPreviewTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.word_effects_preview_label)) },
                singleLine = true
            )
            if (previewEffects.isEmpty()) {
                Text(
                    text = stringResource(R.string.word_effects_preview_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                WordEffectOverlay(effects = previewEffects, reducedMotion = reducedMotion)
            }
            if (reducedMotion) {
                Text(
                    text = stringResource(R.string.word_effects_reduced_motion_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EffectsSection(
    isLoading: Boolean,
    canRetry: Boolean,
    effects: List<WordEffect>,
    onRetry: () -> Unit,
    onToggle: (WordEffect, Boolean) -> Unit,
    onEdit: (WordEffect) -> Unit,
    onDelete: (WordEffect) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.word_effects_list_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAdd) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.space4))
                Text(stringResource(R.string.word_effects_add))
            }
        }

        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.space16),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            effects.isEmpty() -> Text(
                text = stringResource(R.string.word_effects_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> effects.forEach { effect ->
                WordEffectRow(
                    effect = effect,
                    onToggle = { enabled -> onToggle(effect, enabled) },
                    onEdit = { onEdit(effect) },
                    onDelete = { onDelete(effect) }
                )
            }
        }

        if (canRetry) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.word_effects_retry))
            }
        }
    }
}

@Composable
private fun WordEffectRow(
    effect: WordEffect,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            Text(text = effect.emoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = effect.trigger,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(animationLabel(effect.animation)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = effect.isEnabled, onCheckedChange = onToggle)
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.word_effects_edit)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.word_effects_delete)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordEffectEditorDialog(
    effect: WordEffect?,
    isSaving: Boolean,
    @StringRes errorMessageRes: Int?,
    onDismiss: () -> Unit,
    onSave: (String, String, ChatEffectAnimation) -> Unit
) {
    var trigger by remember(effect?.id) { mutableStateOf(effect?.trigger.orEmpty()) }
    var emoji by remember(effect?.id) { mutableStateOf(effect?.emoji ?: EmojiEffectCatalog.emojis.first()) }
    var animation by remember(effect?.id) { mutableStateOf(effect?.animation ?: ChatEffectAnimation.GLOW) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                stringResource(
                    if (effect == null) R.string.word_effects_add_title else R.string.word_effects_edit_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space12)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.word_effects_trigger_label)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.word_effects_emoji_label)) },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    EmojiEffectCatalog.emojis.forEach { candidate ->
                        FilterChip(
                            selected = candidate == emoji,
                            onClick = { emoji = candidate },
                            label = { Text(candidate) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.word_effects_animation_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    ChatEffectAnimation.entries.forEach { candidate ->
                        FilterChip(
                            selected = candidate == animation,
                            onClick = { animation = candidate },
                            label = { Text(stringResource(animationLabel(candidate))) }
                        )
                    }
                }
                if (errorMessageRes != null) {
                    Text(
                        text = stringResource(errorMessageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trigger, emoji, animation) },
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.word_effects_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.word_effects_cancel))
            }
        }
    )
}

@Composable
private fun DeleteEffectDialog(
    effect: WordEffect,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.word_effects_delete_confirm_title)) },
        text = { Text(stringResource(R.string.word_effects_delete_confirm_body, effect.trigger)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.word_effects_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.word_effects_cancel))
            }
        }
    )
}

@StringRes
private fun animationLabel(animation: ChatEffectAnimation): Int = when (animation) {
    ChatEffectAnimation.GLOW -> R.string.word_effects_animation_glow
    ChatEffectAnimation.CONFETTI -> R.string.word_effects_animation_confetti
    ChatEffectAnimation.SPARKLE -> R.string.word_effects_animation_sparkle
    ChatEffectAnimation.PULSE -> R.string.word_effects_animation_pulse
}
