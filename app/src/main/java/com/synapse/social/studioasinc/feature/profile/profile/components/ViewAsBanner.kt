package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.background
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.foundation.layout.defaultMinSize

@Composable
fun ViewAsBanner(
    viewMode: ViewAsMode,
    specificUserName: String? = null,
    onExitViewAs: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (viewMode) {
                        ViewAsMode.PUBLIC -> "Viewing as Public"
                        ViewAsMode.FRIENDS -> "Viewing as Friends"
                        ViewAsMode.SPECIFIC_USER -> "Viewing as ${specificUserName ?: "User"}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "This is how your profile appears to others",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            TextButton(onClick = onExitViewAs, modifier = Modifier.defaultMinSize(minWidth = Spacing.Huge, minHeight = Spacing.Huge)) {
                Text(stringResource(R.string.action_exit))
            }
        }
    }
}
