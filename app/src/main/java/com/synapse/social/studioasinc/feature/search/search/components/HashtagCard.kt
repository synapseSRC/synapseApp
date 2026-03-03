package com.synapse.social.studioasinc.feature.search.search.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag

@Composable
fun HashtagCard(
    hashtag: SearchHashtag,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${hashtag.tag}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatCount(hashtag.count)} people talking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (hashtag.sparklinePoints.isNotEmpty()) {
            Sparkline(
                points = hashtag.sparklinePoints,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(80.dp)
                    .height(40.dp)
            )
        }
    }
}

@Composable
fun Sparkline(
    points: List<Float>,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val max = points.maxOrNull() ?: 1f
    val min = points.minOrNull() ?: 0f
    val range = if ((max - min) == 0f) 1f else (max - min)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1).coerceAtLeast(1)

        val path = Path()

        points.forEachIndexed { index, value ->
            val x = index * stepX

            val normalizedY = (value - min) / range
            val y = height - (normalizedY * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {

                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
