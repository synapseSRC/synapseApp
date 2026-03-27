package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun getRegionShapeForItem(index: Int, size: Int): Shape {
    val largeRadius = Spacing.Large
    val smallRadius = Spacing.ExtraSmall
    return when {
        size == 1 -> RoundedCornerShape(largeRadius)
        index == 0 -> RoundedCornerShape(topStart = largeRadius, topEnd = largeRadius, bottomStart = smallRadius, bottomEnd = smallRadius)
        index == size - 1 -> RoundedCornerShape(topStart = smallRadius, topEnd = smallRadius, bottomStart = largeRadius, bottomEnd = largeRadius)
        else -> RoundedCornerShape(smallRadius)
    }
}

@Composable
fun RegionItem(
    region: String,
    isSelected: Boolean,
    onRegionSelected: (String) -> Unit,
    shape: Shape
) {

    val targetContainerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )

    Surface(
        shape = shape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Tiny)
            .clickable { onRegionSelected(region) }
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = region,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null
                )
            },
            trailingContent = {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.cd_selected)
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = contentColor,
                leadingIconColor = contentColor,
                trailingIconColor = contentColor
            )
        )
    }
}
