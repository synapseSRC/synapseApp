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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ReviewDeleteSection(largeFiles: List<LargeFileInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Large)
    ) {
        Text(
            text = stringResource(R.string.storage_review_delete),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.Medium)
        )

        val size = formatBytes(largeFiles.sumOf { it.size })

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.storage_larger_than),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = size,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_document),
                    contentDescription = stringResource(R.string.cd_file_icon),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.cd_chevron_right)
                )
            },
            modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
        )
    }
    HorizontalDivider(thickness = Spacing.Small, color = MaterialTheme.colorScheme.surfaceContainerLowest)
}
