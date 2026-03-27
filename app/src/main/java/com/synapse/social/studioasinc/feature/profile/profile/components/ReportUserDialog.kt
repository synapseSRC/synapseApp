package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.clickable
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ReportUserDialog(
    username: String,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }

    val reasons = listOf(
        "Spam",
        "Harassment or bullying",
        "Hate speech",
        "Violence or dangerous content",
        "Nudity or sexual content",
        "False information",
        "Impersonation",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_user_title, username)) },
        text = {
            LazyColumn {
                item {
                    Text(
                        "Why are you reporting this user?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.Small)
                    )
                }
                items(reasons) { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = Spacing.Small)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(reason, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedReason?.let { onReport(it) }
                    onDismiss()
                },
                enabled = selectedReason != null
            ) {
                Text(stringResource(R.string.action_report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
