package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(
    displayName: String,
    scrollProgress: Float,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (scrollProgress > 0.5f) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (scrollProgress > 0.7f) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "titleAlpha"
    )

    val iconTint by animateColorAsState(
        targetValue = if (scrollProgress > 0.5f) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )

    TopAppBar(
        title = {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha }
            )
        },
        navigationIcon = {
            AnimatedIconButton(
                onClick = onBackClick,
                contentDescription = "Back"
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        },
        actions = {
            AnimatedIconButton(
                onClick = onMoreClick,
                contentDescription = "More options"
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = iconTint,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = iconTint
        ),
        modifier = modifier
    )
}

@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = Modifier
            .scale(scale)
            .semantics { this.contentDescription = contentDescription }
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileTopAppBarPreview() {
    MaterialTheme {
        ProfileTopAppBar(
            displayName = "John Doe",
            scrollProgress = 0.7f,
            onBackClick = {},
            onMoreClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileTopAppBarTransparentPreview() {
    MaterialTheme {
        ProfileTopAppBar(
            displayName = "John Doe",
            scrollProgress = 0f,
            onBackClick = {},
            onMoreClick = {}
        )
    }
}
