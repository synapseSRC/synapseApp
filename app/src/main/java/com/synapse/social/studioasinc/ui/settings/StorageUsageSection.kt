package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun StorageUsageSection(storageUsage: StorageUsageBreakdown) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium)
    ) {
        val totalGB = formatBytesToGB(storageUsage.totalSize)
        val usedGB = formatBytesToGB(storageUsage.usedSize)
        val freeGB = formatBytesToGB(storageUsage.freeSize)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.storage_used, usedGB),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.storage_free, freeGB),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.Small))

        StorageBar(usage = storageUsage)

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(stringResource(R.string.storage_synapse_media), style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Badge(color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(stringResource(R.string.storage_apps_other), style = MaterialTheme.typography.bodyMedium)
        }
    }
    HorizontalDivider(thickness = Spacing.Small, color = MaterialTheme.colorScheme.surfaceContainerLowest)
}

@Composable
private fun StorageBar(usage: StorageUsageBreakdown) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(Spacing.Small)
        .clip(MaterialTheme.shapes.small)
    ) {
        val totalWidth = size.width
        val synapseWidth = (usage.synapseSize.toFloat() / usage.totalSize) * totalWidth
        val otherWidth = (usage.appsAndOtherSize.toFloat() / usage.totalSize) * totalWidth

        drawRect(color = surfaceVariant.copy(alpha = 0.3f))

        drawLine(
            color = primaryColor,
            start = Offset(0f, size.height / 2),
            end = Offset(synapseWidth, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )

        drawLine(
            color = tertiaryColor,
            start = Offset(synapseWidth, size.height / 2),
            end = Offset(synapseWidth + otherWidth, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun Badge(color: androidx.compose.ui.graphics.Color) {
    val cdStorageBadge = stringResource(id = R.string.cd_storage_badge)
    Box(
        modifier = Modifier
            .size(Spacing.Small)
            .background(color, CircleShape)
            .semantics { contentDescription = cdStorageBadge }
    )
}
