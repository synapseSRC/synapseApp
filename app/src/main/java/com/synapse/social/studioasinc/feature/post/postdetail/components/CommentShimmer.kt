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
    modifier: Modifier = Modifier
) {
    val avatarSize = 40.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)
        ) {
            // Left Column: Avatar
            Box(
                modifier = Modifier.width(avatarSize),
                contentAlignment = Alignment.TopCenter
            ) {
                ShimmerCircle(size = avatarSize)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column: Content
            Column(modifier = Modifier.weight(1f)) {
                // Header (Username and Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.width(100.dp).height(14.dp))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    ShimmerBox(modifier = Modifier.width(40.dp).height(14.dp))
                }

                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

                // Text Content
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.95f).height(14.dp))
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))

                // Interaction Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Items: Comment, Repost, Like, Views, Bookmark, Share
                    repeat(4) {
                        ShimmerBox(modifier = Modifier.width(32.dp).height(18.dp))
                    }
                    ShimmerBox(modifier = Modifier.size(18.dp))
                    ShimmerBox(modifier = Modifier.size(18.dp))
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
