package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.NumberFormatter
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun StatItem(
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "statItemScale"
    )

    val formattedCount = NumberFormatter.formatCount(count)
    val contentDesc = "$formattedCount $label"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)
            .minimumInteractiveComponentSize()
            .scale(scale)
            .semantics {
                contentDescription = contentDesc
            }
    ) {
        Text(
            text = formattedCount,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            textAlign = TextAlign.Center
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun StatsRow(
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    onStatsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        horizontalArrangement = if (content != null) Arrangement.Start else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (content != null) {
            content()
        } else {
            StatItem(
                count = postsCount,
                label = stringResource(R.string.posts),
                onClick = { onStatsClick("posts") },
                modifier = Modifier.weight(1f)
            )

            StatItem(
                count = followersCount,
                label = stringResource(R.string.followers),
                onClick = { onStatsClick("followers") },
                modifier = Modifier.weight(1f)
            )

            StatItem(
                count = followingCount,
                label = stringResource(R.string.following),
                onClick = { onStatsClick("following") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatItemPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            StatItem(
                count = 1234,
                label = "Followers",
                onClick = { }
            )
            StatItem(
                count = 567,
                label = "Following",
                onClick = { }
            )
        }
    }
}
