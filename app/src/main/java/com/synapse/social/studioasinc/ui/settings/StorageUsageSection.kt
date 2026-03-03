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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun StorageUsageSection(storageUsage: StorageUsageBreakdown) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val totalGB = formatBytesToGB(storageUsage.totalSize)
        val usedGB = formatBytesToGB(storageUsage.usedSize)
        val freeGB = formatBytesToGB(storageUsage.freeSize)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$usedGB used",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$freeGB free",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))


        StorageBar(usage = storageUsage)

        Spacer(modifier = Modifier.height(16.dp))


        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Synapse media", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.width(16.dp))

            Badge(color = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apps and other items", style = MaterialTheme.typography.bodySmall)
        }
    }
    HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLowest)
}

@Composable
private fun StorageBar(usage: StorageUsageBreakdown) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(MaterialTheme.shapes.small)
    ) {
        val totalWidth = size.width
        val synapseWidth = (usage.synapseSize.toFloat() / usage.totalSize) * totalWidth
        val otherWidth = (usage.appsAndOtherSize.toFloat() / usage.totalSize) * totalWidth


        drawRect(color = Color.LightGray.copy(alpha = 0.3f))


        drawLine(
            color = Color(0xFF6750A4),
            start = Offset(0f, size.height / 2),
            end = Offset(synapseWidth, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )


        drawLine(
            color = Color(0xFF7D5260),
            start = Offset(synapseWidth, size.height / 2),
            end = Offset(synapseWidth + otherWidth, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Butt
        )
    }
}

@Composable
private fun Badge(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}
