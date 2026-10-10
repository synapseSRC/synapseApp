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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun StorageUsageSection(
    storageUsage: StorageUsageBreakdown,
    modifier: Modifier = Modifier
) {
    SettingsSection(
        title = stringResource(R.string.storage_section_usage),
        modifier = modifier
    ) {
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SettingsSpacing.itemHorizontalPadding)
            ) {
                val usedGB = formatBytesToGB(storageUsage.usedSize)
                val freeGB = formatBytesToGB(storageUsage.freeSize)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.storage_used, usedGB),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.storage_free, freeGB),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Medium))

                StorageBar(usage = storageUsage)

                Spacer(modifier = Modifier.height(Spacing.Medium))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Badge(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = stringResource(R.string.storage_synapse_media),
                        style = SettingsTypography.itemSubtitle
                    )

                    Spacer(modifier = Modifier.width(Spacing.Large))

                    Badge(color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = stringResource(R.string.storage_apps_other),
                        style = SettingsTypography.itemSubtitle
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageBar(usage: StorageUsageBreakdown) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val barDescription = stringResource(R.string.cd_storage_badge) + ": " +
            formatBytesToGB(usage.synapseSize) + " " + stringResource(R.string.storage_synapse_media) + ", " +
            formatBytesToGB(usage.appsAndOtherSize) + " " + stringResource(R.string.storage_apps_other)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(MaterialTheme.shapes.small)
            .semantics(mergeDescendants = true) {
                contentDescription = barDescription
            }
    ) {
        val totalWidth = size.width
        val safeTotal = if (usage.totalSize > 0) usage.totalSize.toFloat() else 1f
        val synapseWidth = (usage.synapseSize.toFloat() / safeTotal) * totalWidth
        val otherWidth = (usage.appsAndOtherSize.toFloat() / safeTotal) * totalWidth

        drawRect(color = trackColor)

        if (synapseWidth > 0) {
            drawLine(
                color = primaryColor,
                start = Offset(0f, size.height / 2),
                end = Offset(synapseWidth, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
        }

        if (otherWidth > 0) {
            drawLine(
                color = tertiaryColor,
                start = Offset(synapseWidth, size.height / 2),
                end = Offset((synapseWidth + otherWidth).coerceAtMost(totalWidth), size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Butt
            )
        }
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
