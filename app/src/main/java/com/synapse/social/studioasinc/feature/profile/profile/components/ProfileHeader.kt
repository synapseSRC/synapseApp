package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.components.ButtonVariant
import com.synapse.social.studioasinc.feature.shared.components.ExpressiveButton
import com.synapse.social.studioasinc.feature.shared.components.animatedShape
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.domain.model.UserStatus

@Composable
fun ProfileHeader(
    avatar: String?,
    status: UserStatus?,
    coverImageUrl: String?,
    name: String?,
    username: String,
    nickname: String?,
    bio: String?,
    isVerified: Boolean,
    hasStory: Boolean,
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    isOwnProfile: Boolean,
    onProfileImageClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onMoreClick: () -> Unit,
    onStatsClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFollowing: Boolean = false,
    isFollowLoading: Boolean = false,
    onFollowClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onCoverEditClick: () -> Unit = {},
    onCoverPhotoClick: () -> Unit = {},
    scrollOffset: Float = 0f,
    bioExpanded: Boolean = false,
    onToggleBio: () -> Unit = {}
) {
    val coverHeight = Sizes.HeightExtraLarge
    val overlap = Sizes.HeightMedium
    val contentPaddingTop = coverHeight - overlap
    val avatarSize = Sizes.AvatarHuge
    val avatarPaddingTop = contentPaddingTop - (avatarSize / 2)
    val textSpacerTop = (avatarSize / 2) + Spacing.SmallMedium
    val avatarBorderWidth = Spacing.ExtraSmall

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        CoverPhoto(
            coverImageUrl = coverImageUrl,
            scrollOffset = scrollOffset,
            isOwnProfile = isOwnProfile,
            onEditClick = onCoverEditClick,
            onCoverClick = onCoverPhotoClick,
            height = coverHeight
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = contentPaddingTop)
                .clip(RoundedCornerShape(topStart = Sizes.CornerMassive, topEnd = Sizes.CornerMassive))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium)
            ) {
                Spacer(modifier = Modifier.height(textSpacerTop))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                    ) {
                        Text(
                            text = name ?: username,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isVerified) {
                            AnimatedVerifiedBadge()
                        }
                    }

                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!nickname.isNullOrBlank()) {
                        Text(
                            text = nickname,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    InlineStatsText(
                        postsCount = postsCount,
                        followersCount = followersCount,
                        followingCount = followingCount,
                        onStatsClick = onStatsClick
                    )
                }

                // Add spacing before bio
                Spacer(modifier = Modifier.height(Spacing.Medium))

                if (!bio.isNullOrBlank()) {
                    ExpandableBio(
                        bio = bio,
                        expanded = bioExpanded,
                        onToggle = onToggleBio
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                }



                ProfileActionButtons(
                    isOwnProfile = isOwnProfile,
                    isFollowing = isFollowing,
                    isFollowLoading = isFollowLoading,
                    onEditProfileClick = onEditProfileClick,
                    onAddStoryClick = onAddStoryClick,
                    onFollowClick = onFollowClick,
                    onMessageClick = onMessageClick,
                    onMoreClick = onMoreClick
                )

                Spacer(modifier = Modifier.height(Spacing.Medium))
            }
        }

        // Avatar overlapping equally
        Box(
            modifier = Modifier
                .padding(start = Spacing.Medium)
                .padding(top = avatarPaddingTop)
        ) {
            ProfileImageWithRing(
                avatar = avatar,
                size = avatarSize,
                status = status,
                hasStory = hasStory,
                isOwnProfile = isOwnProfile,
                onClick = onProfileImageClick,
                modifier = Modifier.border(avatarBorderWidth, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
@Composable
fun AnimatedVerifiedBadge(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "verifiedBadge")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "verifiedScale"
    )

    Surface(
        modifier = modifier
            .size(Sizes.IconLarge)
            .scale(scale),
        shape = SevenSidedCookieShape(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = stringResource(R.string.verified_account),
                modifier = Modifier.size(Sizes.IconSemiMedium)
            )
        }
    }
}

@Composable
private fun ExpandableBio(
    bio: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldCollapse = bio.length > 150

    Column(modifier = modifier) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) + expandVertically())
                    .togetherWith(fadeOut(animationSpec = tween(200)) + shrinkVertically())
            },
            label = "bioExpand"
        ) { isExpanded ->
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (isExpanded || !shouldCollapse) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = shouldCollapse) { onToggle() }
            )
        }

        if (shouldCollapse) {
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = if (expanded) stringResource(R.string.show_less) else stringResource(R.string.see_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onToggle() }
            )
        }
    }
}

@Composable
private fun ProfileActionButtons(
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onEditProfileClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOwnProfile) {
            ExpressiveButton(
                onClick = onAddStoryClick,
                text = stringResource(R.string.add_story),
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.ButtonHeight),
                variant = ButtonVariant.Filled
            )

            ExpressiveButton(
                onClick = onEditProfileClick,
                text = stringResource(R.string.edit_profile),
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.ButtonHeight),
                variant = ButtonVariant.FilledTonal
            )
        } else {
            AnimatedFollowButton(
                isFollowing = isFollowing,
                isLoading = isFollowLoading,
                onClick = onFollowClick,
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.ButtonHeight)
            )

            ExpressiveButton(
                onClick = onMessageClick,
                text = stringResource(R.string.m_message),
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.ButtonHeight),
                variant = ButtonVariant.Outlined
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedFollowButton(
    isFollowing: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val containerColor by animateColorAsState(
        targetValue = if (isFollowing) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "followButtonColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isFollowing) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onPrimary
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "followButtonContentColor"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        enabled = !isLoading,
        shape = ButtonDefaults.animatedShape(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.7f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
             if (isLoading) {
                 CircularProgressIndicator(
                     modifier = Modifier.size(Sizes.IconMedium),
                     strokeWidth = Sizes.BorderDefault,
                     color = contentColor
                 )
             } else {
                 AnimatedContent(
                    targetState = isFollowing,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f))
                            .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f))
                    },
                    label = "followButtonContent"
                ) { following ->
                    Text(
                        text = if (following) stringResource(R.string.following) else stringResource(R.string.follow),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
             }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileHeaderPreview() {
    MaterialTheme {
        ProfileHeader(
            avatar = null,
            status = UserStatus.ONLINE,
            coverImageUrl = null,
            name = "John Doe",
            username = "johndoe",
            nickname = "JD",
            bio = "Software developer | Tech enthusiast | Coffee lover ☕️ | Building amazing things with code every day.",
            isVerified = true,
            hasStory = true,
            postsCount = 142,
            followersCount = 12345,
            followingCount = 567,
            isOwnProfile = true,
            onProfileImageClick = {},
            onEditProfileClick = {},
            onAddStoryClick = {},
            onMoreClick = {},
            onStatsClick = {}
        )
    }
}





@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineStatsText(
    postsCount: Int,
    followersCount: Int,
    followingCount: Int,
    onStatsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedFollowers = com.synapse.social.studioasinc.core.util.NumberFormatter.formatCount(followersCount)
    val formattedFollowing = com.synapse.social.studioasinc.core.util.NumberFormatter.formatCount(followingCount)
    val formattedPosts = com.synapse.social.studioasinc.core.util.NumberFormatter.formatCount(postsCount)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.clickable { onStatsClick("followers") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedFollowers,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(
                text = stringResource(R.string.followers).lowercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "  ·  ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.clickable { onStatsClick("following") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedFollowing,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(
                text = stringResource(R.string.following).lowercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "  ·  ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.clickable { onStatsClick("posts") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedPosts,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(
                text = stringResource(R.string.posts).lowercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
