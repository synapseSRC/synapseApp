package com.synapse.social.studioasinc.core.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A filled button that morphs its shape from a rounded pill to a cookie shape when pressed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shapes = ButtonDefaults.shapes(
            pressedShape = MaterialShapes.Cookie4Sided.toShape()
        ),
        content = { content() }
    )
}

/**
 * A toggle button that morphs between three shapes:
 * - Idle: rounded pill
 * - Pressed: cookie shape
 * - Checked: clover shape
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        shapes = ToggleButtonDefaults.shapes(
            pressedShape = MaterialShapes.Cookie4Sided.toShape(),
            checkedShape = MaterialShapes.Clover4Leaf.toShape()
        ),
        content = { content() }
    )
}

/**
 * A filled icon toggle button that morphs between three shapes:
 * - Idle: circle
 * - Pressed: cookie shape
 * - Checked: clover shape
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        shapes = IconButtonDefaults.toggleableShapes(
            pressedShape = MaterialShapes.Cookie4Sided.toShape(),
            checkedShape = MaterialShapes.Clover4Leaf.toShape()
        ),
        content = { content() }
    )
}
