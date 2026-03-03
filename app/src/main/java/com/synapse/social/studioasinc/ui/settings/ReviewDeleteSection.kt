package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

@Composable
fun ReviewDeleteSection(largeFiles: List<LargeFileInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Review and delete items",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Larger than 5 MB") },
            supportingContent = { Text(formatBytes(largeFiles.sumOf { it.size })) },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_document),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = null)
            },
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
    HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLowest)
}
