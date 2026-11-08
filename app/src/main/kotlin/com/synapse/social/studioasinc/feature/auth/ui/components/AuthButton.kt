package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil

/**
 * Primary action button for the authentication flow.
 *
 * The appearance parameters below are optional and default to the original scheme-driven styling,
 * so existing call sites keep their current look; screens that need a brand-specific treatment
 * (such as the welcome screen) can override colour, shape, height and label style without
 * forking this component. Press animation and haptics are always preserved.
 */
@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = ButtonDefaults.shape,
    height: Dp = Sizes.ButtonHeightLarge,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    elevation: ButtonElevation? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current


    val scale = AnimationUtil.animatedScale(pressed = isPressed)


    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        shape = shape,
        elevation = elevation,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
    ) {
        if (loading) {
            ExpressiveLoadingIndicator(
                modifier = Modifier.size(Sizes.IconLarge),
                color = contentColor
            )
        } else {
            Text(
                text = text,
                style = textStyle
            )
        }
    }
}

