package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.GroupedListSpacing
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.settings.SettingsAnimations
import kotlinx.coroutines.launch

enum class ChatAttachmentCategory(
    @StringRes val titleRes: Int,
    @StringRes val supportingTextRes: Int,
    val icon: ImageVector
) {
    PHOTOS_AND_VIDEOS(R.string.chat_attachment_photos_videos, R.string.chat_attachment_photos_videos_supporting, Icons.Rounded.PhotoLibrary),
    DOCUMENTS(R.string.chat_attachment_documents, R.string.chat_attachment_documents_supporting, Icons.Rounded.Description),
    AUDIO(R.string.chat_attachment_audio, R.string.chat_attachment_audio_supporting, Icons.Rounded.Headphones),
    FILES(R.string.chat_attachment_files, R.string.chat_attachment_files_supporting, Icons.Rounded.FolderOpen),
    CONTACTS(R.string.chat_attachment_contacts, R.string.chat_attachment_contacts_supporting, Icons.Rounded.ContactPage)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatAttachmentSheet(
    onDismissRequest: () -> Unit,
    onCategorySelected: (ChatAttachmentCategory) -> Unit
) {
    // Keep all five choices visible in the requested compact-phone sheet.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AppColors.screenBackground,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenPadding, vertical = Spacing.space16)
        ) {
            Text(
                text = stringResource(R.string.chat_attachment_share_content),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Spacing.space12))

            val categories = ChatAttachmentCategory.values()
            Column(verticalArrangement = Arrangement.spacedBy(GroupedListSpacing)) {
                categories.forEachIndexed { index, category ->
                    AttachmentCategoryItem(
                        category = category,
                        position = GroupedItemPosition.at(index, categories.size),
                        onClick = { dismissThen { onCategorySelected(category) } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AttachmentCategoryItem(
    category: ChatAttachmentCategory,
    position: GroupedItemPosition,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) SettingsAnimations.pressScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "attachmentCategoryPress"
    )
    val shape = position.shape

    ListItem(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppSpacing.rowMinHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        leadingContent = {
            Surface(
                modifier = Modifier.size(AppSpacing.iconBadgeSize),
                shape = CircleShape,
                color = AppColors.iconContainerBackground,
                contentColor = AppColors.itemIcon
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppSpacing.rowIconSize)
                    )
                }
            }
        },
        content = {
            Text(
                text = stringResource(category.titleRes),
                style = AppTypography.itemTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = stringResource(category.supportingTextRes),
                style = AppTypography.itemSubtitle,
                color = AppColors.supportingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        verticalAlignment = Alignment.CenterVertically,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape
        ),
        colors = ListItemDefaults.colors(
            containerColor = AppColors.cardBackground,
            headlineColor = AppColors.itemContent,
            leadingIconColor = AppColors.itemIcon,
            supportingColor = AppColors.supportingText
        ),
        contentPadding = PaddingValues(
            horizontal = AppSpacing.rowHorizontalPadding,
            vertical = AppSpacing.rowVerticalPadding
        ),
        interactionSource = interactionSource
    )
}
