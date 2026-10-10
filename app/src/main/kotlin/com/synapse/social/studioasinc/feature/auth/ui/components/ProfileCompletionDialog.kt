package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ProfileCompletionDialog(
    onComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.complete_profile_title_dialog)) },
        text = {
            Text(stringResource(R.string.complete_profile_body))
        },
        confirmButton = {
            Button(onClick = onComplete) {
                Text(stringResource(R.string.complete_profile_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_later))
            }
        }
    )
}
