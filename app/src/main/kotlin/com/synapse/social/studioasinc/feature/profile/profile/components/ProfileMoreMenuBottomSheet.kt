package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMoreMenuBottomSheet(
    isOwnProfile: Boolean,
    onDismiss: () -> Unit,
    onShareProfile: () -> Unit,
    onViewAs: () -> Unit,
    onLockProfile: () -> Unit,
    onSettings: () -> Unit,
    onBlockUser: () -> Unit,
    onReportUser: () -> Unit,
    onMuteUser: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.space16,
                    end = Spacing.space16,
                    bottom = Spacing.space16
                )
        ) {
            // 3dp gaps and 28/8dp stacked corners are the contained-list treatment in the spec.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val itemCount = 4

                ProfileMenuItem(
                    index = 0,
                    count = itemCount,
                    icon = Icons.Rounded.Share,
                    title = stringResource(R.string.profile_menu_share_title),
                    supporting = stringResource(R.string.profile_menu_share_supporting),
                    onClick = { dismissThen(onShareProfile) }
                )

                if (isOwnProfile) {
                    ProfileMenuItem(
                        index = 1,
                        count = itemCount,
                        icon = Icons.Rounded.Visibility,
                        title = stringResource(R.string.profile_menu_view_as_title),
                        supporting = stringResource(R.string.profile_menu_view_as_supporting),
                        onClick = { dismissThen(onViewAs) }
                    )
                    ProfileMenuItem(
                        index = 2,
                        count = itemCount,
                        icon = Icons.Rounded.Lock,
                        title = stringResource(R.string.profile_menu_lock_title),
                        supporting = stringResource(R.string.profile_menu_lock_supporting),
                        onClick = { dismissThen(onLockProfile) }
                    )
                    ProfileMenuItem(
                        index = 3,
                        count = itemCount,
                        icon = Icons.Rounded.Settings,
                        title = stringResource(R.string.profile_menu_settings_title),
                        supporting = stringResource(R.string.profile_menu_settings_supporting),
                        onClick = { dismissThen(onSettings) }
                    )
                } else {
                    ProfileMenuItem(
                        index = 1,
                        count = itemCount,
                        icon = Icons.Rounded.Block,
                        title = stringResource(R.string.profile_menu_block_title),
                        supporting = stringResource(R.string.profile_menu_block_supporting),
                        onClick = { dismissThen(onBlockUser) }
                    )
                    ProfileMenuItem(
                        index = 2,
                        count = itemCount,
                        icon = Icons.Rounded.Flag,
                        title = stringResource(R.string.profile_menu_report_title),
                        supporting = stringResource(R.string.profile_menu_report_supporting),
                        onClick = { dismissThen(onReportUser) }
                    )
                    ProfileMenuItem(
                        index = 3,
                        count = itemCount,
                        icon = Icons.AutoMirrored.Rounded.VolumeOff,
                        title = stringResource(R.string.profile_menu_mute_title),
                        supporting = stringResource(R.string.profile_menu_mute_supporting),
                        onClick = { dismissThen(onMuteUser) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileMenuItem(
    index: Int,
    count: Int,
    icon: ImageVector,
    title: String,
    supporting: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "profileMenuItemPress"
    )

    val shape = when {
        count == 1 -> RoundedCornerShape(Sizes.CornerMassive)
        index == 0 -> RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive,
            bottomStart = Sizes.CornerMedium,
            bottomEnd = Sizes.CornerMedium
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = Sizes.CornerMedium,
            topEnd = Sizes.CornerMedium,
            bottomStart = Sizes.CornerMassive,
            bottomEnd = Sizes.CornerMassive
        )
        else -> RoundedCornerShape(Sizes.CornerMedium)
    }

    ListItem(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.HeightAvatarLarge)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        leadingContent = {
            Surface(
                modifier = Modifier.size(Sizes.IconMassive),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconLarge)
                    )
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconLarge)
            )
        },
        // The 72dp minimum is above the ListItem vertical-alignment breakpoint, whose default
        // falls back to Alignment.Top, so the content is pinned explicitly to the row centre.
        verticalAlignment = Alignment.CenterVertically,
        supportingContent = {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        shapes = ListItemDefaults.shapes(
            shape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape
        ),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            leadingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(
            start = Spacing.space16,
            end = Spacing.space16,
            top = Spacing.space4,
            bottom = Spacing.space4
        ),
        interactionSource = interactionSource,
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
