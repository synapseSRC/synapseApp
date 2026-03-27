package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.settings.ApiKeyInfo
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ApiKeyItem(
    apiKey: ApiKeyInfo,
    onDelete: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SettingsSpacing.itemPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = apiKey.keyName,
                style = SettingsTypography.itemTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = "${apiKey.provider.uppercase()} • ${apiKey.usageCount}/${apiKey.usageLimit ?: "∞"} used",
                style = SettingsTypography.itemSubtitle,
                color = SettingsColors.itemIcon
            )
        }

        IconButton(
            onClick = { showDeleteConfirm = true }
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = SettingsColors.destructiveButton
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_api_key_title)) },
            text = { Text(stringResource(R.string.delete_api_key_body, apiKey.keyName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(apiKey.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SettingsColors.destructiveButton)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
