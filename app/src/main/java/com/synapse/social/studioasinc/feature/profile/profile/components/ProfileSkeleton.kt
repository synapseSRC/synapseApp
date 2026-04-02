package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun ProfileHeaderShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.HeightExtraLarge),
            shape = RoundedCornerShape(0.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Spacing.Medium)
                .offset(y = Sizes.AvatarHugeHalfOffset) // half of AvatarHuge
        ) {
            ShimmerCircle(size = Sizes.AvatarHuge)
        }
    }
}

@Composable
fun ProfileBioShimmer(displayName: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.Medium)
    ) {
        Spacer(modifier = Modifier.height(Sizes.AvatarHugeOffset))

        if (displayName != null) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            ShimmerBox(
                modifier = Modifier
                    .width(Sizes.ShimmerWidthExtraLarge)
                    .height(Sizes.ShimmerTextMedium)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        ShimmerBox(
            modifier = Modifier
                .width(Sizes.ShimmerWidthMedium)
                .height(Spacing.MediumLarge)
        )

        Spacer(modifier = Modifier.height(Spacing.Small))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(Sizes.ShimmerTextSmall)
        )
        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(Sizes.ShimmerTextSmall)
        )
    }
}

@Composable
fun ProfileStatsShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.Medium)
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(Sizes.ShimmerTextSmall)
        )
    }
}

@Composable
fun ProfileActionsShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(Spacing.ButtonHeight),
                shape = RoundedCornerShape(Sizes.CornerExtraLarge)
            )
        }
    }
}

@Composable
fun ProfileDetailsShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.Medium)
    ) {
        ShimmerBox(
            modifier = Modifier
                .width(Sizes.ShimmerWidthMedium)
                .height(Sizes.ShimmerTextMedium)
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(Sizes.ShimmerTextSmall)
            )
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        }
    }
}

@Composable
fun FollowingSectionShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        repeat(4) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShimmerCircle(size = Sizes.AvatarLarge)
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                ShimmerBox(
                    modifier = Modifier
                        .width(Sizes.AvatarLarge)
                        .height(Sizes.ShimmerTextSmall)
                )
            }
        }
    }
}

@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)
        ) {
            Box(
                modifier = Modifier.width(Spacing.Huge),
                contentAlignment = Alignment.TopCenter
            ) {
                ShimmerCircle(size = Sizes.IconGiant)
            }

            Spacer(modifier = Modifier.width(Spacing.SmallMedium))

            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(
                    modifier = Modifier
                        .width(Sizes.ShimmerWidthLarge)
                        .height(Spacing.MediumLarge)
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                ShimmerBox(
                    modifier = Modifier
                        .width(Sizes.ShimmerWidthSmallMedium)
                        .height(Sizes.ShimmerTextSmall)
                )

                Spacer(modifier = Modifier.height(Spacing.Small))

                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(Sizes.ShimmerTextSmall)
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(Sizes.ShimmerTextSmall)
                )

                Spacer(modifier = Modifier.height(Spacing.Small))

                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.HeightExtraLarge)
                )

                Spacer(modifier = Modifier.height(Spacing.SmallMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(Spacing.ExtraLarge)
                                .height(Sizes.HeightSmall)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Small))
    }
}

@Composable
fun PhotoGridSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.Tiny)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Tiny)
            ) {
                repeat(3) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(Sizes.CornerSmall)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Tiny))
        }
    }
}
