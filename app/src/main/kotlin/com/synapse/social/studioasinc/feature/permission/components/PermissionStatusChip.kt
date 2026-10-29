package com.synapse.social.studioasinc.feature.permission.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import com.synapse.social.studioasinc.feature.permission.icon
import com.synapse.social.studioasinc.feature.permission.labelRes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes

@Composable
internal fun PermissionStatusChip(
    status: PermissionStatus,
    modifier: Modifier = Modifier
) {
    val containerColor = when (status) {
        PermissionStatus.GRANTED -> MaterialTheme.colorScheme.secondaryContainer
        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED -> MaterialTheme.colorScheme.errorContainer
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when (status) {
        PermissionStatus.GRANTED -> MaterialTheme.colorScheme.onSecondaryContainer
        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED -> MaterialTheme.colorScheme.onErrorContainer
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = AppShapes.chipShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconSmall)
            )
            Text(
                text = stringResource(status.labelRes()),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
