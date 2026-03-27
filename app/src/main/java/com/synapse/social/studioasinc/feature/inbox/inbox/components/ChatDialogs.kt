package com.synapse.social.studioasinc.feature.inbox.inbox.components

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
