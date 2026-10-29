package com.synapse.social.studioasinc.feature.echo.lobby.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.echo.lobby.EchoCreateUiState
import com.synapse.social.studioasinc.feature.echo.messageRes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility
import kotlin.math.roundToInt

/**
 * The create-room form.
 *
 * It is a form, not a wizard: the title is the only required field, and the defaults (public,
 * eight speakers) describe the most common room. Validation failures and backend refusals both
 * arrive as [EchoCreateUiState.error], already mapped to copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEchoRoomSheet(
    state: EchoCreateUiState,
    onTitleChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onVisibilityChange: (EchoVisibility) -> Unit,
    onMaxSpeakersChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.space20)
                .padding(bottom = Spacing.space24),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {
            Text(
                text = stringResource(R.string.echo_create_title),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.echo_create_name_label)) },
                placeholder = { Text(stringResource(R.string.echo_create_name_placeholder)) },
                supportingText = {
                    Text(
                        stringResource(
                            R.string.echo_create_name_counter,
                            state.title.length,
                            EchoCreateUiState.MAX_TITLE_LENGTH
                        )
                    )
                },
                singleLine = true,
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.topic,
                onValueChange = onTopicChange,
                label = { Text(stringResource(R.string.echo_create_topic_label)) },
                placeholder = { Text(stringResource(R.string.echo_create_topic_placeholder)) },
                supportingText = {
                    Text(
                        stringResource(
                            R.string.echo_create_name_counter,
                            state.topic.length,
                            EchoCreateUiState.MAX_TOPIC_LENGTH
                        )
                    )
                },
                minLines = 2,
                maxLines = 3,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                Text(
                    text = stringResource(R.string.echo_create_visibility_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    FilterChip(
                        selected = state.visibility == EchoVisibility.PUBLIC,
                        onClick = { onVisibilityChange(EchoVisibility.PUBLIC) },
                        enabled = !state.isSubmitting,
                        label = { Text(stringResource(R.string.echo_visibility_public)) }
                    )
                    FilterChip(
                        selected = state.visibility == EchoVisibility.UNLISTED,
                        onClick = { onVisibilityChange(EchoVisibility.UNLISTED) },
                        enabled = !state.isSubmitting,
                        label = { Text(stringResource(R.string.echo_visibility_unlisted)) }
                    )
                }
                Text(
                    text = stringResource(
                        if (state.visibility == EchoVisibility.PUBLIC) {
                            R.string.echo_visibility_public_hint
                        } else {
                            R.string.echo_visibility_unlisted_hint
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.echo_create_speakers_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(
                            R.string.echo_create_speakers_value,
                            state.maxSpeakers
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Slider(
                    value = state.maxSpeakers.toFloat(),
                    onValueChange = { onMaxSpeakersChange(it.roundToInt()) },
                    valueRange = EchoCreateUiState.MIN_SPEAKERS.toFloat()..EchoCreateUiState.MAX_SPEAKERS.toFloat(),
                    steps = EchoCreateUiState.MAX_SPEAKERS - EchoCreateUiState.MIN_SPEAKERS - 1,
                    enabled = !state.isSubmitting
                )
            }

            if (state.error != null) {
                Text(
                    text = stringResource(state.error.messageRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.echo_create_cancel))
                }
                Button(
                    onClick = onCreate,
                    enabled = state.title.isNotBlank() && !state.isSubmitting
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Sizes.IconSmall),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = Sizes.BorderDefault
                        )
                    } else {
                        Text(stringResource(R.string.echo_create_action))
                    }
                }
            }
        }
    }
}
