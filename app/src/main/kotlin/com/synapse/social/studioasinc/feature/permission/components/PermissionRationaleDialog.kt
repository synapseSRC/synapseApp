package com.synapse.social.studioasinc.feature.permission.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.feature.permission.purposeRes
import com.synapse.social.studioasinc.feature.permission.titleRes

/**
 * Shown only when a permission was denied once but the system would still ask again. A first request
 * launches directly because the card itself already explains why the permission is wanted.
 */
@Composable
internal fun PermissionRationaleDialog(
    permission: AppPermission,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.permission_rationale_title,
                    stringResource(permission.titleRes())
                )
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.permission_rationale_message,
                    stringResource(permission.purposeRes())
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.permission_rationale_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.permission_rationale_dismiss))
            }
        }
    )
}
