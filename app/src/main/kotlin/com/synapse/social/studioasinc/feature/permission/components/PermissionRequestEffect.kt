package com.synapse.social.studioasinc.feature.permission.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.synapse.social.studioasinc.feature.permission.PendingPermissionRequest

/**
 * Drives exactly one system permission dialog per [PendingPermissionRequest].
 *
 * Keying on `requestKey` is what makes a repeat request possible while an answer that was just given
 * cannot reopen the dialog. `launched` lives in the ViewModel, so a rotation during the dialog does not
 * show a second one.
 */
@Composable
internal fun PermissionRequestEffect(
    pending: PendingPermissionRequest?,
    onLaunched: () -> Unit,
    onResult: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onResult() }

    LaunchedEffect(pending?.requestKey) {
        val request = pending ?: return@LaunchedEffect
        if (request.launched) return@LaunchedEffect
        onLaunched()
        launcher.launch(request.permission.manifestName)
    }
}
