package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.animateFloatAsState
import android.content.Context
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.BatchMediaCardMetrics
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment
// M3 Expressive default effects spring (damping 1.0, stiffness 1600): opacity does not overshoot.
private val batchMediaOpacitySpring = spring<Float>(
    dampingRatio = 1.0f,
    stiffness = 1600f
)

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3_600

private fun mediaThumbnailRequest(context: Context, attachment: MessageAttachment): ImageRequest =
    ImageRequest.Builder(context)
        .data(attachment.url)
        .crossfade(true)
        .apply {
            if (attachment.type == AttachmentType.VIDEO) {
                videoFramePercent(0.1)
            }
        }
        .build()

internal data class StackedMediaCardDimensions(
    val stackWidth: Dp,
    val stackHeight: Dp,
    val cardWidth: Dp,
    val cardHeight: Dp
)

internal fun stackedMediaCardDimensions(availableWidth: Dp): StackedMediaCardDimensions {
    val maxStackWidth = BatchMediaCardMetrics.MaxFrontWidth + BatchMediaCardMetrics.StackHorizontalReserve
    val boundedWidth = if (availableWidth == Dp.Infinity) {
        maxStackWidth
    } else {
        availableWidth.coerceAtLeast(0.dp)
    }
    val stackWidth = boundedWidth.coerceAtMost(maxStackWidth)
    val cardWidth = (stackWidth - BatchMediaCardMetrics.StackHorizontalReserve)
        .coerceIn(0.dp, BatchMediaCardMetrics.MaxFrontWidth)
    val cardHeight = cardWidth / BatchMediaCardMetrics.AspectRatio
    val stackHeight = if (cardWidth > 0.dp) {
        cardHeight + BatchMediaCardMetrics.StackVerticalReserve
    } else {
        0.dp
    }

    return StackedMediaCardDimensions(
        stackWidth = stackWidth,
        stackHeight = stackHeight,
        cardWidth = cardWidth,
        cardHeight = cardHeight
    )
}

internal fun stackedBackgroundCardCount(attachmentCount: Int): Int =
    (attachmentCount - 1).coerceIn(0, 2)

internal fun formatVideoDuration(durationSeconds: Int): String {
    val duration = durationSeconds.coerceAtLeast(0)
    val hours = duration / SECONDS_PER_HOUR
    val minutes = (duration % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = (duration % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$seconds"
    } else {
        "$minutes:$seconds"
    }
}

@Composable
fun StackedMediaCard(
    attachments: List<MessageAttachment>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    val totalCount = attachments.size
    val backgroundCardCount = stackedBackgroundCardCount(totalCount)
    val frontItem = attachments.first()
    val isVideo = frontItem.type == AttachmentType.VIDEO
    val stackDescription = stringResource(R.string.chat_cd_stacked_media, totalCount)
    val context = LocalContext.current
    val frontImageRequest = remember(frontItem.url, frontItem.type, context) {
        mediaThumbnailRequest(context, frontItem)
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val dimensions = stackedMediaCardDimensions(maxWidth)
        val frontWidth = dimensions.cardWidth
        val frontHeight = dimensions.cardHeight

        Box(
            modifier = Modifier
                .width(dimensions.stackWidth)
                .height(dimensions.stackHeight)
                .semantics { contentDescription = stackDescription }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // MessageAttachment currently has no reliable pixel-resolution/quality fields, so no HD badge is shown.
            if (backgroundCardCount >= 2) {
                val bg2Item = attachments.getOrNull(2)
                val progress2 = bg2Item?.uploadProgress ?: 1.0f
                val targetOpacity2 = (0.35f + (progress2 * 0.35f)).coerceIn(0.35f, 0.7f)
                val animatedOpacity2 by animateFloatAsState(
                    targetValue = targetOpacity2,
                    animationSpec = batchMediaOpacitySpring,
                    label = "card_opacity_2"
                )

                Box(
                    modifier = Modifier
                        .size(
                            width = frontWidth * BatchMediaCardMetrics.RearScale,
                            height = frontHeight * BatchMediaCardMetrics.RearScale
                        )
                        .offset(
                            x = BatchMediaCardMetrics.RearOffsetX,
                            y = BatchMediaCardMetrics.RearOffsetY
                        )
                        .rotate(BatchMediaCardMetrics.RearRotationDegrees)
                        .shadow(
                            elevation = BatchMediaCardMetrics.RearShadowElevation,
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        )
                        .clip(RoundedCornerShape(Sizes.CornerMedium))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(
                            width = Sizes.BorderThin,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        )
                ) {
                    if (bg2Item != null) {
                        val bg2ImageRequest = remember(bg2Item.url, bg2Item.type, context) {
                            mediaThumbnailRequest(context, bg2Item)
                        }
                        AsyncImage(
                            model = bg2ImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(animatedOpacity2)
                        )
                    }
                }
            }

            if (backgroundCardCount >= 1) {
                val bg1Item = attachments.getOrNull(1)
                val progress1 = bg1Item?.uploadProgress ?: 1.0f
                val targetOpacity1 = (0.4f + (progress1 * 0.45f)).coerceIn(0.4f, 0.85f)
                val animatedOpacity1 by animateFloatAsState(
                    targetValue = targetOpacity1,
                    animationSpec = batchMediaOpacitySpring,
                    label = "card_opacity_1"
                )

                Box(
                    modifier = Modifier
                        .size(
                            width = frontWidth * BatchMediaCardMetrics.MiddleScale,
                            height = frontHeight * BatchMediaCardMetrics.MiddleScale
                        )
                        .offset(
                            x = BatchMediaCardMetrics.MiddleOffsetX,
                            y = BatchMediaCardMetrics.MiddleOffsetY
                        )
                        .rotate(BatchMediaCardMetrics.MiddleRotationDegrees)
                        .shadow(
                            elevation = BatchMediaCardMetrics.MiddleShadowElevation,
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        )
                        .clip(RoundedCornerShape(Sizes.CornerMedium))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = Sizes.BorderThin,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        )
                ) {
                    if (bg1Item != null) {
                        val bg1ImageRequest = remember(bg1Item.url, bg1Item.type, context) {
                            mediaThumbnailRequest(context, bg1Item)
                        }
                        AsyncImage(
                            model = bg1ImageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(animatedOpacity1)
                        )
                    }
                }
            }

            val frontProgress = frontItem.uploadProgress
            val frontTargetOpacity = (0.4f + (frontProgress * 0.6f)).coerceIn(0.4f, 1.0f)
            val animatedFrontOpacity by animateFloatAsState(
                targetValue = frontTargetOpacity,
                animationSpec = batchMediaOpacitySpring,
                label = "front_card_opacity"
            )

            // Rotate only the media surface; controls and badges remain upright for readability.
            Box(
                modifier = Modifier.size(width = frontWidth, height = frontHeight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(BatchMediaCardMetrics.FrontRotationDegrees)
                        .shadow(
                            elevation = BatchMediaCardMetrics.FrontShadowElevation,
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        )
                        .clip(RoundedCornerShape(Sizes.CornerMedium))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(
                            width = Sizes.BorderDefault,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(Sizes.CornerMedium)
                        )
                ) {
                    AsyncImage(
                        model = frontImageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(animatedFrontOpacity)
                    )
                }

                if (frontItem.isUploadFailed || attachments.any { it.isUploadFailed }) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(Sizes.IconExtraLarge)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.upload_failed),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(Sizes.IconLarge)
                        )
                    }
                } else if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(Sizes.IconGiant)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.chat_action_play_video),
                            tint = Color.White,
                            modifier = Modifier.size(Sizes.IconLarge)
                        )
                    }

                    frontItem.duration?.takeIf { it > 0 }?.let { duration ->
                        val durationLabel = formatVideoDuration(duration)
                        val durationDescription = stringResource(R.string.chat_cd_video_duration, durationLabel)
                        Surface(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(Sizes.CornerSmall),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = Spacing.Medium, bottom = Spacing.Small)
                                .semantics { contentDescription = durationDescription }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.ExtraSmall,
                                    vertical = Spacing.Tiny
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(Sizes.IconSmall)
                                )
                                Spacer(modifier = Modifier.width(Spacing.Tiny))
                                Text(
                                    text = durationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(Sizes.CornerSmall),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.Small)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = Spacing.ExtraSmall,
                            vertical = Spacing.Tiny
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(Sizes.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Tiny))
                        Text(
                            text = totalCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
