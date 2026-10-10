package com.synapse.social.studioasinc.feature.blocking.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.synapse.social.studioasinc.R

/**
 * Confirmation dialog for unblocking a user.
 * 
 * Displays an AlertDialog asking the user to confirm the unblock action.
 * Uses theme-compliant styling and string resources for all text.
 * 
 * Validates Requirements 4.2, 8.4
 * 
 * @param onConfirm Callback when user confirms the unblock action
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
fun UnblockConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.unblock_user_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = stringResource(R.string.unblock_user_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.unblock),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Preview
@Composable
private fun UnblockConfirmationDialogPreview() {
    MaterialTheme {
        UnblockConfirmationDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
