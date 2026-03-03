package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaQualityBottomSheet(
    onDismissRequest: () -> Unit,
    currentQuality: MediaUploadQuality,
    onQualitySelected: (MediaUploadQuality) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Photo upload quality",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(SettingsSpacing.itemHorizontalPadding)
            )
            MediaUploadQuality.values().forEach { quality ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQualitySelected(quality) }
                        .padding(horizontal = SettingsSpacing.itemHorizontalPadding, vertical = SettingsSpacing.itemVerticalPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentQuality == quality,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                    Column {
                        Text(text = quality.displayName(), style = MaterialTheme.typography.bodyLarge)
                        Text(text = quality.description(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun AutoDownloadDialog(
    title: String,
    selectedTypes: Set<MediaType>,
    onConfirm: (Set<MediaType>) -> Unit,
    onDismiss: () -> Unit
) {
    val tempSelection = remember(selectedTypes) { mutableStateListOf(*selectedTypes.toTypedArray()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                MediaType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (tempSelection.contains(type)) {
                                    tempSelection.remove(type)
                                } else {
                                    tempSelection.add(type)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tempSelection.contains(type),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                        Text(type.displayName())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelection.toSet()) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
