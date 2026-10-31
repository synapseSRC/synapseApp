package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ChatReportDialog(
    isGroupChat: Boolean,
    targetName: String?,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val context = LocalContext.current
    val reasonResIds = remember {
        listOf(
            R.string.report_reason_spam,
            R.string.report_reason_harassment,
            R.string.report_reason_hate_speech,
            R.string.report_reason_violence,
            R.string.report_reason_misinformation,
            R.string.report_reason_other
        )
    }
    var selectedReasonRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_menu_report)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = if (isGroupChat) {
                        stringResource(
                            R.string.chat_report_group_confirm,
                            targetName ?: stringResource(R.string.group_chat_fallback_title)
                        )
                    } else {
                        stringResource(
                            R.string.chat_info_report_user_confirm,
                            targetName ?: stringResource(R.string.user)
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Spacing.space12))
                Text(
                    text = stringResource(R.string.select_report_reason),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reasonResIds.forEach { reasonRes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReasonRes = reasonRes }
                            .padding(vertical = Spacing.space4),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReasonRes == reasonRes,
                            onClick = { selectedReasonRes = reasonRes }
                        )
                        Spacer(modifier = Modifier.width(Spacing.space8))
                        Text(
                            text = stringResource(reasonRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedReasonRes?.let { onReport(context.getString(it)) }
                    onDismiss()
                },
                enabled = selectedReasonRes != null
            ) {
                Text(stringResource(R.string.action_report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
