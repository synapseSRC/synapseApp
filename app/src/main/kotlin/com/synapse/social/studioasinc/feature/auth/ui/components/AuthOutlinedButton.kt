package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Outlined secondary action shared by every alternative sign-in method - the OAuth providers and
 * the passkey entry point - so they cannot drift apart in height, corner radius, border, icon
 * placement, press animation or haptics.
 *
 * A minimum height rather than a fixed height is used on purpose: large font scales grow the button
 * instead of clipping the label.
 */
@Composable
fun AuthOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale = AnimationUtil.animatedScale(pressed = isPressed)

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Sizes.ButtonHeightM3Medium)
            .scale(scale),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(Sizes.BorderThin, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space8)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    leadingIcon()
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                // Symmetric inset keeps the label optically centred while reserving the icon lane,
                // so a long label can never run under the icon.
                modifier = Modifier.padding(
                    horizontal = if (leadingIcon != null) Sizes.IconLarge + Spacing.space8 else 0.dp
                )
            )
        }
    }
}

