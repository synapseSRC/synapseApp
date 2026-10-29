package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16, vertical = Spacing.space16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(Sizes.HeightDefault),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chat_attachment_share_content),
                    modifier = Modifier.weight(1f),
                    // Product-specific bold emphasis; size stays on the M3 titleLarge scale.
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(Spacing.space12))
            // My design decision (not in M3): use the requested 3dp gaps for this contained list.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                ChatAttachmentCategory.values().forEachIndexed { index, category ->
                    AttachmentCategoryItem(
                        category = category,
                        index = index,
                        itemCount = ChatAttachmentCategory.values().size,
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
    index: Int,
    itemCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // My design decision (not in M3): a subtle press scale is requested for tappable rows.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "attachmentCategoryPress"
    )
    val shape = when {
        itemCount == 1 -> RoundedCornerShape(Sizes.CornerMassive)
        index == 0 -> RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive,
            bottomStart = Sizes.CornerMedium,
            bottomEnd = Sizes.CornerMedium
        )
        index == itemCount - 1 -> RoundedCornerShape(
            topStart = Sizes.CornerMedium,
            topEnd = Sizes.CornerMedium,
            bottomStart = Sizes.CornerMassive,
            bottomEnd = Sizes.CornerMassive
        )
        else -> RoundedCornerShape(Sizes.CornerMedium)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(Sizes.HeightAvatarLarge).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Default),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(category.supportingTextRes),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Default),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(Sizes.IconMassive),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(Sizes.IconLarge)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
