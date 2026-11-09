package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.call.components.CallIcons
import com.synapse.social.studioasinc.feature.call.components.callEndReasonLabel
import com.synapse.social.studioasinc.feature.call.components.formatCallDuration
import com.synapse.social.studioasinc.feature.call.components.spokenDuration
import com.synapse.social.studioasinc.feature.shared.theme.DarkError
import com.synapse.social.studioasinc.feature.shared.theme.DarkErrorContainer
import com.synapse.social.studioasinc.feature.shared.theme.DarkOnError
import com.synapse.social.studioasinc.feature.shared.theme.DarkOnErrorContainer
import com.synapse.social.studioasinc.feature.shared.theme.DarkOnPrimary
import com.synapse.social.studioasinc.feature.shared.theme.DarkOnTertiaryContainer
import com.synapse.social.studioasinc.feature.shared.theme.DarkPrimary
import com.synapse.social.studioasinc.feature.shared.theme.DarkTertiaryContainer
import com.synapse.social.studioasinc.feature.shared.theme.LightError
import com.synapse.social.studioasinc.feature.shared.theme.LightErrorContainer
import com.synapse.social.studioasinc.feature.shared.theme.LightOnError
import com.synapse.social.studioasinc.feature.shared.theme.LightOnErrorContainer
import com.synapse.social.studioasinc.feature.shared.theme.LightOnPrimary
import com.synapse.social.studioasinc.feature.shared.theme.LightOnTertiaryContainer
import com.synapse.social.studioasinc.feature.shared.theme.LightPrimary
import com.synapse.social.studioasinc.feature.shared.theme.LightTertiaryContainer
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.callEndReasonFromRaw
import com.synapse.social.studioasinc.shared.domain.model.call.isMissedCall
import com.synapse.social.studioasinc.shared.domain.model.chat.*

/**
 * M3 Expressive card component for Contact sharing.
 * Uses inner radius = outer - padding (12dp) and tertiaryContainer for avatar.
 */
@Composable
fun ContactCardBubble(
    metadata: ContactMetadata?,
    contentColor: Color,
    onContactAction: (String) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
    ) {
        Column(modifier = Modifier.padding(Spacing.space12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!metadata?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = metadata.avatarUrl,
                        contentDescription = "Contact Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                    )
                } else {
                    val initials = metadata?.name?.split(" ")
                        ?.mapNotNull { it.firstOrNull()?.toString() }
                        ?.take(2)
                        ?.joinToString("")
                        ?.uppercase()
                        ?.ifBlank { "C" } ?: "C"
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Spacing.space12))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata?.name ?: "Contact",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    metadata?.phoneNumber?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.space8))
            FilledTonalButton(
                onClick = { metadata?.phoneNumber?.let { onContactAction(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(
                    text = "Add Contact",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * M3 Expressive Map location card.
 * Uses extraLarge corners (28dp) for outer media frame, and inner 16dp radius.
 */
@Composable
fun MapCardBubble(
    metadata: MapMetadata?,
    contentColor: Color,
    onOpenMap: (Double, Double) -> Unit = { _, _ -> }
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable {
                metadata?.let { onOpenMap(it.latitude, it.longitude) }
            }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map Icon",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(modifier = Modifier.padding(Spacing.space8)) {
                Text(
                    text = metadata?.locationName ?: "Map Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata != null) {
                    Spacer(modifier = Modifier.height(Spacing.space4))
                    Text(
                        text = "Lat: ${metadata.latitude}, Lon: ${metadata.longitude}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * M3 Expressive Location card.
 */
@Composable
fun LocationCardBubble(
    metadata: LocationMetadata?,
    contentColor: Color,
    onOpenMap: (Double, Double) -> Unit = { _, _ -> }
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable {
                metadata?.let { onOpenMap(it.latitude, it.longitude) }
            }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Pin",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(44.dp)
                )
            }
            Column(modifier = Modifier.padding(Spacing.space8)) {
                Text(
                    text = metadata?.title ?: "Location Share",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                metadata?.address?.let {
                    Spacer(modifier = Modifier.height(Spacing.space4))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * M3 Expressive Live Location card with pulsing dot animation.
 */
@Composable
fun LiveLocationCardBubble(
    metadata: LiveLocationMetadata?,
    contentColor: Color,
    onOpenLiveMap: () -> Unit = {}
) {
    val isLive = metadata?.isLive == true
    val infiniteTransition = rememberInfiniteTransition(label = "liveLocationPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable { onOpenLiveMap() }
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLive) MaterialTheme.colorScheme.errorContainer.copy(alpha = pulseAlpha)
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Live Location Indicator",
                    tint = if (isLive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Live Location",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = if (isLive) "Live updating..." else "Live location ended",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLive) MaterialTheme.colorScheme.error else contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * M3 Expressive Poll card bubble.
 * Uses surfaceContainerHigh for unselected rows, secondaryContainer for selected rows.
 */
@Composable
fun PollCardBubble(
    metadata: PollMetadata?,
    messageId: String,
    contentColor: Color,
    onVoteOption: (messageId: String, optionId: String) -> Unit = { _, _ -> }
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
    ) {
        Column(modifier = Modifier.padding(Spacing.space12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Poll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(
                    text = metadata?.question ?: "Poll",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(Spacing.space8))
            metadata?.options?.forEach { option ->
                val totalVotes = metadata.options.sumOf { it.voteCount }.coerceAtLeast(1)
                val percentage = (option.voteCount.toFloat() / totalVotes)
                val isSelected = option.voterUserIds.isNotEmpty()

                val rowBg = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                val rowText = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rowBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.space4)
                        .clickable { onVoteOption(messageId, option.id) }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percentage)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space8),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = rowText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${option.voteCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = rowText
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * M3 Expressive Music track card.
 * Uses 48dp album art with extraSmall (4dp) corner radius.
 */
@Composable
fun MusicCardBubble(
    metadata: MusicMetadata?,
    contentColor: Color,
    onPlayAudio: (String) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = metadata?.albumArtUrl,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.title ?: "Audio Track",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metadata?.artist ?: "Artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { metadata?.audioUrl?.let { onPlayAudio(it) } }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Track",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * M3 Expressive Shared Post Card.
 * Outer shape: largeIncreased (24dp) or 16dp with outlineVariant border.
 */
@Composable
fun SharedPostCardBubble(
    metadata: SharedPostMetadata?,
    contentColor: Color,
    onOpenPost: (String) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable { metadata?.postId?.let { onOpenPost(it) } }
    ) {
        Column(modifier = Modifier.padding(Spacing.space8)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = metadata?.authorAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(
                    text = metadata?.authorName ?: "User Post",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            metadata?.mediaUrl?.let { url ->
                Spacer(modifier = Modifier.height(Spacing.space8))
                AsyncImage(
                    model = url,
                    contentDescription = "Post Media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            metadata?.content?.let { text ->
                Spacer(modifier = Modifier.height(Spacing.space4))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * M3 Expressive Shared Story Card.
 */
@Composable
fun SharedStoryCardBubble(
    metadata: SharedStoryMetadata?,
    contentColor: Color,
    onOpenStory: (String) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable { metadata?.storyId?.let { onOpenStory(it) } }
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = metadata?.mediaUrl ?: metadata?.authorAvatarUrl,
                    contentDescription = "Story Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${metadata?.authorName ?: "User"}'s Story",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = "Tap to view story",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * M3 Expressive Event Card.
 * Uses largeIncreased (24dp) shape or 16dp inner shape.
 */
@Composable
fun EventCardBubble(
    metadata: EventMetadata?,
    contentColor: Color,
    onOpenEvent: (String) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable { metadata?.eventId?.let { onOpenEvent(it) } }
    ) {
        Column(modifier = Modifier.padding(Spacing.space12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(
                    text = metadata?.title ?: "Event",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(Spacing.space4))
            metadata?.startTimeIso?.let {
                Text(
                    text = "📅 $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            metadata?.location?.let {
                Text(
                    text = "📍 $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * M3 Expressive Product Item Card.
 */
@Composable
fun ProductCardBubble(
    metadata: ProductMetadata?,
    contentColor: Color,
    onOpenProduct: (String) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable { metadata?.productId?.let { onOpenProduct(it) } }
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = metadata?.imageUrl,
                contentDescription = "Product Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.title ?: "Product",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = "${metadata?.currency ?: "$"} ${metadata?.price ?: "0.00"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * M3 Expressive Payment transaction card.
 * Uses tertiaryContainer fill with onTertiaryContainer content, and headlineMediumEmphasized amount.
 */
@Composable
fun PaymentCardBubble(
    metadata: PaymentMetadata?,
    contentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payment ${metadata?.status ?: "COMPLETED"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "${metadata?.currency ?: "$"} ${metadata?.amount ?: "0.00"}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/**
 * M3 Expressive Code Snippet block.
 * Uses surfaceContainerHighest background, onSurface text, thin outlineVariant border, monospace typography, and top-right language chip.
 */
@Composable
fun CodeSnippetCardBubble(
    metadata: CodeSnippetMetadata?,
    contentColor: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
    ) {
        Column(modifier = Modifier.padding(Spacing.space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                metadata?.filename?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = metadata?.language?.uppercase() ?: "CODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.space8))
            Text(
                text = metadata?.code ?: "",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Call log message.
 *
 * Wraps its content at the chat bubble radius and carries the timestamp / delivery status in the
 * trailing corner. Its colors are pinned to the static scheme rather than read off
 * [MaterialTheme.colorScheme] so the same call outcome cannot change hue per device or chat theme.
 */
@Composable
fun CallLogCardBubble(
    metadata: CallMetadata?,
    isFromMe: Boolean,
    cornerRadius: Int,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.(Color) -> Unit)? = null
) {
    val isIncoming = !isFromMe
    val reason = callEndReasonFromRaw(metadata?.callStatus)
    val isVideo = metadata?.isVideo == true
    val durationMs = (metadata?.durationSeconds ?: 0) * 1000L
    val isMissed = isMissedCall(reason = reason, connected = durationMs > 0L, isIncoming = isIncoming)
    val statusLabel = callEndReasonLabel(reason, isIncoming = isIncoming)
    val hasDuration = durationMs > 0L &&
        (reason == CallEndReason.COMPLETED || reason == CallEndReason.REMOTE_ENDED)

    // Material You re-derives the tertiary role per wallpaper, which rendered one completed call
    // purple on a Pixel emulator and cyan on a Galaxy. The neutral roles stay neutral either way.
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isMissed) {
        if (isDark) DarkErrorContainer else LightErrorContainer
    } else {
        if (isDark) DarkTertiaryContainer else LightTertiaryContainer
    }
    val textColor = if (isMissed) {
        if (isDark) DarkOnErrorContainer else LightOnErrorContainer
    } else {
        if (isDark) DarkOnTertiaryContainer else LightOnTertiaryContainer
    }
    // A solid semantic disc instead of a neutral one: the grey circle sat too close to the
    // container fill to read as a separate element at a glance.
    val iconDiscColor = if (isMissed) {
        if (isDark) DarkError else LightError
    } else {
        if (isDark) DarkPrimary else LightPrimary
    }
    val iconTint = if (isMissed) {
        if (isDark) DarkOnError else LightOnError
    } else {
        if (isDark) DarkOnPrimary else LightOnPrimary
    }

    // The spoken form spells the duration out; the visual "0:13" reads as "oh thirteen".
    val subtitle = if (hasDuration) {
        stringResource(R.string.call_summary_line, statusLabel, formatCallDuration(durationMs))
    } else {
        statusLabel
    }
    val subtitleDescription = if (hasDuration) {
        stringResource(R.string.call_summary_line, statusLabel, spokenDuration(durationMs))
    } else {
        null
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(cornerRadius.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.space12,
                top = Spacing.space8,
                end = Spacing.space16,
                bottom = Spacing.space8
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.IconMassive)
                    .clip(CircleShape)
                    .background(iconDiscColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMissed) CallIcons.missed else if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Sizes.IconLarge)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.space8))
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(
                            if (isVideo) R.string.chat_action_video_call else R.string.chat_action_voice_call
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Spacing.space4))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (subtitleDescription != null) {
                            Modifier.semantics { contentDescription = subtitleDescription }
                        } else {
                            Modifier
                        }
                    )
                }
                footer?.let { composeFooter ->
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Column { composeFooter(textColor) }
                }
            }
        }
    }
}

/**
 * M3 Expressive View Once / Ephemeral Media card.
 * Expired / consumed state uses errorContainer background, crossed-eye / timer icon, and "Media expired" label.
 */
@Composable
fun ViewOnceCardBubble(
    metadata: ViewOnceMetadata?,
    mediaUrl: String?,
    contentColor: Color,
    onOpenEphemeralMedia: (String) -> Unit = {}
) {
    val isConsumed = metadata?.isConsumed == true

    val cardBg = if (isConsumed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    val cardContentColor = if (isConsumed) MaterialTheme.colorScheme.onErrorContainer else contentColor

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
            .clickable {
                if (!isConsumed && mediaUrl != null) {
                    onOpenEphemeralMedia(mediaUrl)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConsumed) Icons.Default.VisibilityOff else Icons.Default.Timer,
                contentDescription = null,
                tint = if (isConsumed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConsumed) "Media expired" else if (metadata?.mode == "VIEW_TWICE") "View Twice Media" else "View Once Media",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cardContentColor
                )
                Text(
                    text = if (isConsumed) "Opened" else "Tap to view",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConsumed) cardContentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * M3 Expressive File / Document attachment card.
 * Uses inner radius = outer - padding (12dp) and file-type icons.
 */
@Composable
fun FileMessageBubble(
    message: Message,
    contentColor: Color,
    downloadFile: (String, String?) -> Unit = { _, _ -> },
    openUrl: (String) -> Unit = {}
) {
    val attachment = message.attachments.firstOrNull()
    val url = attachment?.url ?: message.mediaUrl

    val (fileName, fileExt, fileMimeType) = filenameForFile(message, attachment)
    val fileSizeStr = attachment?.size?.let { size ->
        when {
            size >= 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            size >= 1024 -> "${size / 1024} KB"
            else -> "$size B"
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space4)
    ) {
        Box(
            modifier = Modifier.clickable { url?.let { openUrl(it) } }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(Spacing.space8)
            ) {
                val icon = fileIconForExtension(fileExt)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.chat_cd_file),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.space8))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(fileSizeStr, fileMimeType.takeIf { it.isNotBlank() }).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.space8))
                IconButton(
                    onClick = { url?.let { downloadFile(url, fileName) } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download File",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun filenameForFile(message: Message, attachment: MessageAttachment?): Triple<String, String, String> {
    val url = attachment?.url ?: message.mediaUrl ?: ""
    val content = message.content

    if (content.isNotBlank()) {
        val ext = content.substringAfterLast('.', "")
        val mimeType = if (ext.isNotBlank()) {
            android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else {
            null
        }
        return Triple(content, if (ext.isNotBlank()) ".$ext" else "", mimeType ?: "")
    }

    if (url.isNotBlank()) {
        val path = url.substringBeforeLast('?')
        val fileName = path.substringAfterLast('/')
        if (fileName.isNotBlank()) {
            val ext = fileName.substringAfterLast('.', "")
            val mimeType = if (ext.isNotBlank()) {
                android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            } else {
                null
            }
            return Triple(fileName, if (ext.isNotBlank()) ".$ext" else "", mimeType ?: "")
        }
    }

    return Triple(stringResource(R.string.chat_media_attachment), "", "")
}

private fun fileIconForExtension(extension: String): ImageVector {
    return when (extension.lowercase()) {
        ".pdf" -> Icons.Default.PictureAsPdf
        ".doc", ".docx" -> Icons.Default.Description
        ".xls", ".xlsx" -> Icons.Default.TableChart
        ".ppt", ".pptx" -> Icons.Default.Slideshow
        ".txt" -> Icons.Default.TextFields
        ".zip", ".rar", ".7z", ".tar", ".gz" -> Icons.Default.Archive
        ".mp3", ".wav", ".ogg", ".flac", ".aac" -> Icons.Default.Audiotrack
        ".mp4", ".avi", ".mov", ".mkv", ".webm" -> Icons.Default.VideoLibrary
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp" -> Icons.Default.Image
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
