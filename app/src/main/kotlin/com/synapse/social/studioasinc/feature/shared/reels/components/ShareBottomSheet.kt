package com.synapse.social.studioasinc.feature.shared.reels.components

import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    videoUrl: String,
    onDismiss: () -> Unit,
    onShareExternal: () -> Unit
) {
    val clipLabel = stringResource(R.string.clip_label_post_link)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.space32)
        ) {
            Text(
                text = stringResource(R.string.share_reel),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(Spacing.space16).align(Alignment.CenterHorizontally)
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
                title = stringResource(R.string.copy_link),
                onClick = {
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, videoUrl))) }
                    onDismiss()
                }
            )
        }
    }
}
