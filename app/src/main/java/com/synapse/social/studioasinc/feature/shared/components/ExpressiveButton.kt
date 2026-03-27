package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing






@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    text: String,
    variant: ButtonVariant = ButtonVariant.Filled
) {
    when (variant) {
        ButtonVariant.Filled -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = ButtonDefaults.animatedShape(),
            contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)
        ) {
            ButtonContent(icon, text)
        }

        ButtonVariant.FilledTonal -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = ButtonDefaults.animatedShape(),
            contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)
        ) {
            ButtonContent(icon, text)
        }

        ButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = ButtonDefaults.animatedShape(),
            contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)
        ) {
            ButtonContent(icon, text)
        }

        ButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = ButtonDefaults.animatedShape(),
            contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)
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
            modifier = Modifier.size(Sizes.IconMedium)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
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


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
        content = content
    )
}





@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun androidx.compose.material3.ButtonDefaults.animatedShape(): androidx.compose.ui.graphics.Shape = this.shape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object IconToggleButtonDefaults {
    @Composable
    fun variantAnimatedShapes(): Any = Any()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: Any? = null,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.IconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}
