package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ReportPostDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReason by remember { mutableStateOf("") }

    val reasons = listOf(
        "Spam",
        "Harassment or bullying",
        "Hate speech",
        "Violence or dangerous content",
        "Nudity or sexual content",
        "False information",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_post_title)) },
        text = {
            Column {
                Text(
                    text = "Why are you reporting this post?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.Small)
                )

                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            .padding(vertical = Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(text = reason)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedReason) },
                enabled = selectedReason.isNotEmpty()
            ) {
                Text(stringResource(R.string.action_report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier
    )
}
