package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.domain.model.UserStatus



@Composable
fun CoverPhoto(
    coverImageUrl: String?,
    scrollOffset: Float = 0f,
    isOwnProfile: Boolean = false,
    onEditClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    height: Dp = 200.dp
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {

                translationY = scrollOffset * height.toPx() * 0.5f

                alpha = 1f - scrollOffset
            }
            .clickable(enabled = coverImageUrl != null) { onCoverClick() }
    ) {
        if (coverImageUrl != null) {
            AsyncImage(
                model = coverImageUrl,
                contentDescription = "Cover photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CoverPlaceholder(modifier = Modifier.fillMaxSize())
        }

        if (isOwnProfile) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEditClick()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.SmallMedium)
                    .size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                tonalElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit cover photo",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverPlaceholder(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "coverPlaceholder")

    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(
                        animatedOffset * 500f,
                        animatedOffset * 200f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        500f + animatedOffset * 500f,
                        200f + animatedOffset * 200f
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { alpha = 0.2f },
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}



@Composable
fun CoverPhotoWithProfile(
    coverImageUrl: String?,
    avatar: String?,
    status: UserStatus? = null,
    scrollOffset: Float = 0f,
    isOwnProfile: Boolean = false,
    hasStory: Boolean = false,
    onCoverEditClick: () -> Unit = {},
    onProfileImageClick: () -> Unit = {},
    onCoverClick: () -> Unit = {},
    coverHeight: Dp = 200.dp,
    profileImageSize: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val profileImageOffset = profileImageSize / 2

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        CoverPhoto(
            coverImageUrl = coverImageUrl,
            scrollOffset = scrollOffset,
            isOwnProfile = isOwnProfile,
            onEditClick = onCoverEditClick,
            onCoverClick = onCoverClick,
            height = coverHeight
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Spacing.Medium)
                .offset(y = profileImageOffset)
        ) {
            ProfileImageWithRing(
                avatar = avatar,
                size = profileImageSize,
                status = status,
                hasStory = hasStory,
                isOwnProfile = isOwnProfile,
                onClick = onProfileImageClick
            )
        }
    }
}



@Composable
fun ProfileImageWithRing(
    avatar: String?,
    size: Dp,
    status: UserStatus? = null,
    hasStory: Boolean = false,
    isOwnProfile: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "storyRing")
    val shape = CircleShape

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasStory) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "storyPulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storyRotation"
    )

    val ringWidth = 4.dp
    val ringPadding = 3.dp

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (hasStory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.4f)
                        .graphicsLayer { rotationZ = rotation }
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hasStory) ringWidth else 0.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hasStory) ringWidth + ringPadding else 4.dp)
                .clip(shape)
        ) {
            if (avatar != null) {
                AsyncImage(
                    model = avatar,
                    contentDescription = stringResource(R.string.author_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(size * 0.5f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (status == UserStatus.ONLINE) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = size * 0.05f, end = size * 0.05f)
                    .size(size * 0.25f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(StatusOnline)
                    .semantics {
                        contentDescription = "Online"
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CoverPhotoPreview() {
    MaterialTheme {
        CoverPhoto(
            coverImageUrl = null,
            isOwnProfile = true,
            onEditClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileImageWithRingPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            ProfileImageWithRing(
                avatar = null,
                size = 80.dp,
                status = UserStatus.ONLINE,
                hasStory = true,
                isOwnProfile = false
            )
            ProfileImageWithRing(
                avatar = null,
                size = 80.dp,
                status = UserStatus.OFFLINE,
                hasStory = false,
                isOwnProfile = true
            )
        }
    }
}
