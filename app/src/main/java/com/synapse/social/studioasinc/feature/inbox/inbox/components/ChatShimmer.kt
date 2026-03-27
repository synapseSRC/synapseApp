package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun ChatShimmer(
    modifier: Modifier = Modifier,
    messageCount: Int = 10
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        state = listState,
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        reverseLayout = true
    ) {
        items(messageCount) { index ->
            val isFromMe = index % 3 == 0
            MessageBubbleShimmer(isFromMe = isFromMe)
        }
    }
}

@Composable
private fun MessageBubbleShimmer(
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromMe) {
            ShimmerCircle(
                size = Sizes.IconHuge,
                modifier = Modifier.padding(end = Spacing.Small)
            )
        }
        
        Column(
            modifier = Modifier.widthIn(max = Sizes.BubbleMaxWidth),
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width((120..240).random().dp)
                    .height((40..80).random().dp),
                shape = RoundedCornerShape(
                    topStart = if (isFromMe) Sizes.CornerLarge else Sizes.CornerSmall,
                    topEnd = if (isFromMe) Sizes.CornerSharp else Sizes.CornerLarge,
                    bottomStart = Sizes.CornerLarge,
                    bottomEnd = Sizes.CornerLarge
                )
            )
            
            Spacer(modifier = Modifier.height(Spacing.Tiny))
            
            ShimmerBox(
                modifier = Modifier
                    .width(Sizes.WidthExtraLarge)
                    .height(Sizes.StatusDot),
                shape = RoundedCornerShape(Sizes.CornerSmall)
            )
        }
        
        if (isFromMe) {
            ShimmerCircle(
                size = Sizes.IconHuge,
                modifier = Modifier.padding(start = Spacing.Small)
            )
        }
    }
}
