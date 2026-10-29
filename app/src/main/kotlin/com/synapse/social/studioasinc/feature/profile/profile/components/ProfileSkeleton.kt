package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.ui.graphics.RectangleShape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun ProfileHeaderShimmer(modifier: Modifier = Modifier) {
    val coverHeight = Sizes.HeightExtraLarge
    val overlap = Sizes.HeightMedium
    val contentPaddingTop = coverHeight - overlap
    val avatarSize = Sizes.AvatarHuge
    val avatarPaddingTop = contentPaddingTop - (avatarSize * 0.20f)
    val textSpacerTop = (avatarSize * 0.80f) + Spacing.space12

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight),
            shape = RectangleShape
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
                    .padding(horizontal = Spacing.space16)
            ) {
                Spacer(modifier = Modifier.height(textSpacerTop))
            }
        }

        Box(
            modifier = Modifier
                .padding(start = Spacing.space16)
                .padding(top = avatarPaddingTop)
        ) {
            ShimmerCircle(size = avatarSize)
        }
    }
}

@Composable
fun ProfileBioShimmer(displayName: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.space16)
    ) {
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

        Spacer(modifier = Modifier.height(Spacing.space4))
        ShimmerBox(
            modifier = Modifier
                .width(Sizes.ShimmerWidthMedium)
                .height(Sizes.ShimmerLineHeightLarge)
        )

        Spacer(modifier = Modifier.height(Spacing.space8))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(Sizes.ShimmerTextSmall)
        )
        Spacer(modifier = Modifier.height(Spacing.space4))
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
            .padding(horizontal = Spacing.space16)
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
            .padding(horizontal = Spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(Sizes.ButtonHeight),
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
            .padding(horizontal = Spacing.space16)
    ) {
        ShimmerBox(
            modifier = Modifier
                .width(Sizes.ShimmerWidthMedium)
                .height(Sizes.ShimmerTextMedium)
        )
        Spacer(modifier = Modifier.height(Spacing.space8))
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(Sizes.ShimmerTextSmall)
            )
            Spacer(modifier = Modifier.height(Spacing.space4))
        }
    }
}

@Composable
fun FollowingSectionShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space16)
    ) {
        repeat(4) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShimmerCircle(size = Sizes.AvatarLarge)
                Spacer(modifier = Modifier.height(Spacing.space4))
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
                .padding(horizontal = Spacing.space12, vertical = Spacing.space8)
        ) {
            Box(
                modifier = Modifier.width(Sizes.IconGiant),
                contentAlignment = Alignment.TopCenter
            ) {
                ShimmerCircle(size = Sizes.IconGiant)
            }

            Spacer(modifier = Modifier.width(Spacing.space12))

            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(
                    modifier = Modifier
                        .width(Sizes.ShimmerWidthLarge)
                        .height(Sizes.ShimmerLineHeightLarge)
                )
                Spacer(modifier = Modifier.height(Spacing.space4))
                ShimmerBox(
                    modifier = Modifier
                        .width(Sizes.ShimmerWidthSmallMedium)
                        .height(Sizes.ShimmerTextSmall)
                )

                Spacer(modifier = Modifier.height(Spacing.space8))

                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(Sizes.ShimmerTextSmall)
                )
                Spacer(modifier = Modifier.height(Spacing.space4))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(Sizes.ShimmerTextSmall)
                )

                Spacer(modifier = Modifier.height(Spacing.space8))

                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.HeightExtraLarge)
                )

                Spacer(modifier = Modifier.height(Spacing.space12))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(Sizes.ShimmerWidthTiny)
                                .height(Sizes.HeightSmall)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.space8))
    }
}

