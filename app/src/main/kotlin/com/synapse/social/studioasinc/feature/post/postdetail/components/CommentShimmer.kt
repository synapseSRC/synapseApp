package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle

@Composable
fun CommentShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(itemCount) {
            CommentShimmerItem()
        }
    }
}

@Composable
fun CommentShimmerItem() {
    val avatarSize = Sizes.AvatarMedium

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)
        ) {
            // Left Column: Avatar
            ShimmerCircle(size = avatarSize)

            Spacer(modifier = Modifier.width(Spacing.SmallMedium))

            // Right Column: Header, Content, Interaction Bar
            Column(modifier = Modifier.weight(1f)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(Sizes.ShimmerWidthSmallMedium)
                            .height(Spacing.Medium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    ShimmerBox(
                        modifier = Modifier
                            .width(Sizes.ShimmerWidthMedium)
                            .height(Spacing.SmallMedium)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Small))

                // Content
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

                Spacer(modifier = Modifier.height(Spacing.Medium))

                // Interaction Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(6) {
                        ShimmerCircle(size = Sizes.IconMedium)
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Spacing.SmallMedium),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = Sizes.BorderHairline
        )
    }
}
