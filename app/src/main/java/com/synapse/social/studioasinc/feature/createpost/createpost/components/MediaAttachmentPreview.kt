package com.synapse.social.studioasinc.ui.createpost

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.FeelingType
import com.synapse.social.studioasinc.domain.model.LocationData
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.feature.shared.components.ExpressiveButton
import com.synapse.social.studioasinc.feature.shared.components.ButtonVariant
import com.synapse.social.studioasinc.feature.shared.theme.AccentBlue
import com.synapse.social.studioasinc.feature.shared.theme.AccentYellow
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
@Composable
fun MediaPreviewGrid(
    mediaItems: List<MediaItem>,
    onRemove: (Int) -> Unit,
    onEdit: (Int) -> Unit
) {
    if (mediaItems.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        mediaItems.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.HeightPreview)
            ) {
                MediaItemView(item, onDelete = { onRemove(index) }, onEdit = { onEdit(index) })
            }
        }
    }
}

@Composable
fun MediaItemView(
    item: MediaItem,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Spacing.SmallPlus, end = Spacing.SmallPlus)
                .clip(RoundedCornerShape(Sizes.CornerDefault))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.url,
                contentDescription = stringResource(R.string.cd_attached_media),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (item.type == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.video),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Sizes.IconHuge)
                    )
                }
            }


            if (item.type == MediaType.IMAGE) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.Small)
                        .height(Sizes.HeightButtonSmall),
                    contentPadding = PaddingValues(horizontal = Spacing.Small, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(Sizes.IconSmall))
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.labelSmall)
                }
            }
        }


        Surface(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(Sizes.IconHuge),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = Spacing.ExtraSmall,
            shadowElevation = Sizes.BorderDefault
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_remove),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(Sizes.IconDefault)
                )
            }
        }
    }
}
@Composable
fun PollPreviewCard(poll: PollData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(Sizes.CornerLarge),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small)
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = poll.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(Sizes.IconLarge)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove), modifier = Modifier.size(Sizes.IconSemiMedium))
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
            poll.options.forEach { option ->
                Surface(
                    shape = RoundedCornerShape(Sizes.CornerMedium),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun YoutubePreviewCard(url: String, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(Sizes.CornerLarge),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Sizes.IconHuge))
            Spacer(modifier = Modifier.width(Spacing.SmallMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.label_youtube_video), style = MaterialTheme.typography.labelMedium)
                Text(text = url, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove))
            }
        }
    }
}

@Composable
fun LocationPreviewCard(location: LocationData, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
        shape = RoundedCornerShape(Sizes.CornerLarge),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Sizes.IconHuge))
            Spacer(modifier = Modifier.width(Spacing.SmallMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = location.name, style = MaterialTheme.typography.titleSmall)
                location.address?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove))
            }
        }
    }
}
