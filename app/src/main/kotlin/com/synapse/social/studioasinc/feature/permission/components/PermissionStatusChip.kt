package com.synapse.social.studioasinc.feature.permission.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import com.synapse.social.studioasinc.feature.permission.PermissionColors
import com.synapse.social.studioasinc.feature.permission.PermissionMetrics
import com.synapse.social.studioasinc.feature.permission.icon
import com.synapse.social.studioasinc.feature.permission.labelRes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/** A badge's three colour decisions; the status alone decides them. */
private data class StatusBadgeColors(
    val container: Color,
    val content: Color,
    val border: BorderStroke?
)

/**
 * The live status of one permission.
 *
 * Each state reads differently on purpose: granted uses the screen's teal because it is the state worth
 * rewarding, "not allowed yet" stays neutral because nothing is wrong, a retryable denial is advisory
 * (the shared warning pair), and blocked or restricted takes the scheme's error role. Every pair is a
 * container/on-container or role pairing, so the text keeps its contrast in light, dark and dynamic
 * colour, and the icon repeats what the label says rather than relying on colour alone.
 */
@Composable
internal fun PermissionStatusChip(
    status: PermissionStatus,
    modifier: Modifier = Modifier
) {
    val badge = statusBadgeColors(status)

    Surface(
        modifier = modifier,
        shape = PermissionMetrics.chipShape,
        color = badge.container,
        contentColor = badge.content,
        border = badge.border
    ) {
        Row(
            modifier = Modifier
                .height(PermissionMetrics.chipHeight)
                .padding(horizontal = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
        ) {
            Icon(
                imageVector = status.icon(),
                contentDescription = null,
                modifier = Modifier.size(PermissionMetrics.chipIconSize)
            )
            Text(
                text = stringResource(status.labelRes()),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun statusBadgeColors(status: PermissionStatus): StatusBadgeColors = when (status) {
    PermissionStatus.GRANTED -> StatusBadgeColors(
        container = PermissionColors.grantedContainer,
        content = PermissionColors.grantedContent,
        border = null
    )

    PermissionStatus.NOT_REQUESTED -> StatusBadgeColors(
        container = PermissionColors.neutralContainer,
        content = PermissionColors.neutralContent,
        border = BorderStroke(Sizes.BorderThin, PermissionColors.cardBorder)
    )

    PermissionStatus.DENIED -> StatusBadgeColors(
        container = PermissionColors.attentionContainer,
        content = PermissionColors.attentionContent,
        border = null
    )

    PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED -> StatusBadgeColors(
        container = PermissionColors.blockedContainer,
        content = PermissionColors.blockedContent,
        border = null
    )
}
