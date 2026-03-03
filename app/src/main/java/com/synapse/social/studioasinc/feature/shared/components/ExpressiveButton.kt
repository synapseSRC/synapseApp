package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    text: String,
    variant: ButtonVariant = ButtonVariant.Filled,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    var wasPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            wasPressed -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            wasPressed = false
        } else if (!isPressed && !wasPressed) {
            wasPressed = true
            kotlinx.coroutines.delay(150)
            wasPressed = false
        }
    }

    val buttonColors = when (variant) {
        ButtonVariant.Filled -> ButtonDefaults.buttonColors()
        ButtonVariant.FilledTonal -> ButtonDefaults.filledTonalButtonColors()
        ButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors()
        ButtonVariant.Text -> ButtonDefaults.textButtonColors()
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isPressed)
            buttonColors.containerColor.copy(alpha = 0.85f)
        else buttonColors.containerColor,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "container_color"
    )

    val animatedColors = ButtonDefaults.buttonColors(
        containerColor = animatedContainerColor,
        contentColor = buttonColors.contentColor
    )

    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            colors = animatedColors,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }

        ButtonVariant.FilledTonal -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }

        ButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }

        ButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ButtonContent(icon, text)
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector?, text: String) {
    icon?.let {
        Icon(
            imageVector = it,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

enum class ButtonVariant {
    Filled,
    FilledTonal,
    Outlined,
    Text
}
