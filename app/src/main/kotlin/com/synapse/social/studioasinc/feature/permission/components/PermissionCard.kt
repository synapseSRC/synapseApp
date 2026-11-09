package com.synapse.social.studioasinc.feature.permission.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import com.synapse.social.studioasinc.feature.permission.PermissionColors
import com.synapse.social.studioasinc.feature.permission.PermissionMetrics
import com.synapse.social.studioasinc.feature.permission.PermissionTypography
import com.synapse.social.studioasinc.feature.permission.benefitRes
import com.synapse.social.studioasinc.feature.permission.hintRes
import com.synapse.social.studioasinc.feature.permission.icon
import com.synapse.social.studioasinc.feature.permission.permissionAccent
import com.synapse.social.studioasinc.feature.permission.purposeRes
import com.synapse.social.studioasinc.feature.permission.titleRes
import com.synapse.social.studioasinc.feature.shared.components.pressScale
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor

/**
 * One permission: what it is for, whether the OS has granted it, and the single action its state allows.
 *
 * The action is still decided by the status rather than by the caller — a granted permission offers
 * nothing, a denial can be retried, and a blocked or restricted one can only be changed in system
 * Settings. What changed here is the composition: the permission's accent identifies the card before
 * its title is read, the badge and its glow sit on the same 16dp grid as the card itself, and the
 * status badge carries the difference between "not asked yet", "declined" and "blocked".
 */
@Composable
internal fun PermissionCard(
    snapshot: PermissionSnapshot,
    isRequestPending: Boolean,
    onAction: (AppPermission) -> Unit,
    modifier: Modifier = Modifier
) {
    val permission = snapshot.permission
    val status = snapshot.status
    val accent = permissionAccent(permission, PermissionColors.isDark)
    val cardColor = cardSurfaceColor()
    val cardBorder = PermissionColors.cardBorder
    val cardShadow = PermissionColors.cardShadow
    val glowStrength = PermissionColors.glowStrength
    val strongGlow = accent.glow.copy(alpha = 0.45f * glowStrength)
    val softGlow = accent.glow.copy(alpha = 0.30f * glowStrength)
    val glowFade = accent.glow.copy(alpha = 0f)
    val permissionTitle = stringResource(permission.titleRes())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = PermissionMetrics.cardElevation,
                shape = PermissionMetrics.cardShape,
                clip = false,
                ambientColor = cardShadow,
                spotColor = cardShadow
            )
            .clip(PermissionMetrics.cardShape)
            .background(cardColor)
            .border(Sizes.BorderThin, cardBorder, PermissionMetrics.cardShape)
            .drawBehind {
                // Two casts of the permission's accent, inside the card's own clip so they read as light
                // falling on the surface rather than as a gradient behind the text.
                val accentRadius = size.width * 0.60f
                val accentCentre = Offset(size.width * 0.98f, size.height * 0.06f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(strongGlow, glowFade),
                        center = accentCentre,
                        radius = accentRadius
                    ),
                    center = accentCentre,
                    radius = accentRadius
                )
                val washRadius = size.width * 0.45f
                val washCentre = Offset(size.width * 0.04f, size.height * 0.02f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(softGlow, glowFade),
                        center = washCentre,
                        radius = washRadius
                    ),
                    center = washCentre,
                    radius = washRadius
                )
            }
            .padding(PermissionMetrics.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(PermissionMetrics.badgeSize)
                    .clip(PermissionMetrics.badgeShape)
                    .background(accent.badge),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = permission.icon(),
                    contentDescription = null,
                    tint = accent.onBadge,
                    modifier = Modifier.size(PermissionMetrics.badgeIconSize)
                )
            }

            Spacer(Modifier.width(AppSpacing.iconTextSpacing))

            Column(Modifier.weight(1f)) {
                Text(
                    text = permissionTitle,
                    style = PermissionTypography.cardTitle,
                    color = PermissionColors.heading
                )
                Spacer(Modifier.height(Spacing.space4))
                Text(
                    text = stringResource(permission.purposeRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PermissionColors.body
                )
            }
        }

        PermissionStatusChip(status = status)

        permissionSupportingRes(permission, status)?.let { supportingRes ->
            Text(
                text = stringResource(supportingRes),
                style = MaterialTheme.typography.bodySmall,
                color = PermissionColors.body
            )
        }

        if (status != PermissionStatus.GRANTED) {
            PermissionActionButton(
                status = status,
                permissionTitle = permissionTitle,
                enabled = !isRequestPending,
                onClick = { onAction(permission) }
            )
        }
    }
}

/**
 * At most one supporting line, and the state decides which: before the ask it says what the permission
 * buys, after a refusal it says how to get it back, and a grant needs neither — the granted card is
 * deliberately as quiet as the reference.
 */
private fun permissionSupportingRes(permission: AppPermission, status: PermissionStatus): Int? =
    when (status) {
        PermissionStatus.GRANTED -> null
        PermissionStatus.NOT_REQUESTED -> permission.benefitRes()
        PermissionStatus.DENIED,
        PermissionStatus.PERMANENTLY_DENIED,
        PermissionStatus.RESTRICTED -> status.hintRes()
    }

@Composable
private fun PermissionActionButton(
    status: PermissionStatus,
    permissionTitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (status) {
        PermissionStatus.NOT_REQUESTED -> R.string.permission_action_allow
        PermissionStatus.DENIED -> R.string.permission_action_try_again
        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED ->
            R.string.permission_action_open_settings
        PermissionStatus.GRANTED -> return
    }

    val labelText = stringResource(label)
    val description = stringResource(R.string.permission_action_accessibility, labelText, permissionTitle)
    val interactionSource = remember { MutableInteractionSource() }
    val buttonModifier = modifier
        .height(PermissionMetrics.buttonHeight)
        .pressScale(interactionSource)
        .semantics { contentDescription = description }

    when (status) {
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = PermissionMetrics.buttonShape,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = PermissionColors.action,
                contentColor = PermissionColors.onAction
            )
        ) {
            Text(text = labelText, style = AppTypography.buttonText)
        }

        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = PermissionMetrics.buttonShape,
            interactionSource = interactionSource,
            border = BorderStroke(Sizes.BorderThin, PermissionColors.action.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PermissionColors.action)
        ) {
            Text(text = labelText, style = AppTypography.buttonText)
        }

        PermissionStatus.GRANTED -> Unit
    }
}
