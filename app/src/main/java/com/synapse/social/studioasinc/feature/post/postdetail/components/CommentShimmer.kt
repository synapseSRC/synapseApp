package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val avatarSize = 40.dp

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

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column: Header, Content, Interaction Bar
            Column(modifier = Modifier.weight(1f)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interaction Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(6) {
                        ShimmerCircle(size = 18.dp)
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Spacing.SmallMedium),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }
}
