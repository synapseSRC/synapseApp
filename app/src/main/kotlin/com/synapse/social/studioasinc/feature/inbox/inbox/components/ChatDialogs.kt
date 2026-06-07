package com.synapse.social.studioasinc.feature.inbox.inbox.components
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode

@Composable
fun DisappearingModeDialog(
    currentMode: DisappearingMode,
    onModeSelected: (DisappearingMode) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.disappearing_messages_title)) },
        text = {
            Column {
                val options = listOf(
                    DisappearingMode.OFF to stringResource(R.string.disappearing_mode_off),
                    DisappearingMode.TWENTY_FOUR_HOURS to stringResource(R.string.disappearing_mode_24_hours),
                    DisappearingMode.SEVEN_DAYS to stringResource(R.string.disappearing_mode_7_days)
                )
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = {
                                onModeSelected(mode)
                                onDismissRequest()
                            }
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun MessageSummaryDialog(
    isSummarizingMessage: Boolean,
    messageSummary: String?,
    onDismissRequest: () -> Unit
) {
    if (isSummarizingMessage || messageSummary != null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
            title = { Text(stringResource(R.string.message_summary_title)) },
            text = {
                if (isSummarizingMessage) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(messageSummary ?: "")
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
fun ChatSummaryDialog(
    chatSummary: String?,
    onDismissRequest: () -> Unit
) {
    if (chatSummary != null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.chat_summary_title))
                }
            },
            text = {
                Text(chatSummary ?: "")
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}
