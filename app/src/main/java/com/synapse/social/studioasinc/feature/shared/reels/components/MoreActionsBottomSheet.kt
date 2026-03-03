package com.synapse.social.studioasinc.feature.shared.reels.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreActionsBottomSheet(
    onDismiss: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onDownload: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            ActionItem(
                icon = Icons.Default.Flag,
                title = "Report",
                onClick = {
                    onReport()
                    onDismiss()
                }
            )
            ActionItem(
                icon = Icons.Default.Block,
                title = "Block",
                onClick = {
                    onBlock()
                    onDismiss()
                }
            )
            ActionItem(
                icon = Icons.Default.Download,
                title = "Download",
                onClick = {
                    onDownload()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = title)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
