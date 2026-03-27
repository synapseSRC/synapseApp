package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.components.post.PostOption
import com.synapse.social.studioasinc.feature.shared.components.post.OptionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostMenuBottomSheet(
    isOwnPost: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            val options = mutableListOf<PostOption>()
            if (isOwnPost) {
                options.add(PostOption("Edit Post", Icons.Filled.Edit) {
                    onEdit()
                    onDismiss()
                })
                options.add(PostOption("Delete Post", Icons.Filled.Delete, isDangerous = true) {
                    onDelete()
                    onDismiss()
                })
            } else {
                options.add(PostOption("Report Post", Icons.Filled.Report, isDangerous = true) {
                    onReport()
                    onDismiss()
                })
            }

            LazyColumn {
                items(options) { option ->
                    OptionItem(option = option)
                }
            }
        }
    }
}
