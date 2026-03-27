package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle
import com.synapse.social.studioasinc.ui.inbox.theme.InboxTheme
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun InboxShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 8
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(InboxTheme.dimens.GroupedItemGap)
    ) {
        items(itemCount) { index ->
            val isFirst = index == 0
            val isLast = index == itemCount - 1
            val shape = when {
                itemCount == 1 -> InboxTheme.shapes.GroupedListSingleShape
                isFirst -> InboxTheme.shapes.GroupedListTopShape
                isLast -> InboxTheme.shapes.GroupedListBottomShape
                else -> InboxTheme.shapes.GroupedListMiddleShape
            }
            
            ConversationItemShimmer(
                modifier = Modifier.padding(horizontal = Spacing.Medium),
                shape = shape
            )
        }
    }
}

@Composable
private fun ConversationItemShimmer(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = InboxTheme.shapes.ChatItemCard
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerCircle(size = InboxTheme.dimens.AvatarSize)
        
        Spacer(modifier = Modifier.width(Spacing.Medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(Spacing.Medium),
                    shape = RoundedCornerShape(Sizes.CornerSmall)
                )
                
                ShimmerBox(
                    modifier = Modifier
                        .width(Spacing.ButtonHeight)
                        .height(Spacing.SmallMedium),
                    shape = RoundedCornerShape(Sizes.CornerSmall)
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp),
                shape = RoundedCornerShape(Sizes.CornerSmall)
            )
        }
    }
}
