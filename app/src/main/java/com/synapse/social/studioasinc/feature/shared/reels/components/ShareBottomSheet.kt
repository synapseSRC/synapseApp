package com.synapse.social.studioasinc.feature.shared.reels.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    videoUrl: String,
    onDismiss: () -> Unit,
    onShareExternal: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Share Reel",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )

            ActionItem(
                icon = Icons.Default.Share,
                title = "Share to other apps",
                onClick = {
                    onShareExternal()
                    onDismiss()
                }
            )

            ActionItem(
                icon = Icons.Default.ContentCopy,
                title = "Copy Link",
                onClick = {
                    clipboardManager.setText(AnnotatedString(videoUrl))
                    onDismiss()
                }
            )
        }
    }
}
