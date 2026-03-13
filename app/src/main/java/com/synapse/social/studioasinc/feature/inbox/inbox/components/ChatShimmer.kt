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
                size = 32.dp,
                modifier = Modifier.padding(end = Spacing.Small)
            )
        }
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
        ) {
            ShimmerBox(
                modifier = Modifier
                    .width((120..240).random().dp)
                    .height((40..80).random().dp),
                shape = RoundedCornerShape(
                    topStart = if (isFromMe) 16.dp else 4.dp,
                    topEnd = if (isFromMe) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            ShimmerBox(
                modifier = Modifier
                    .width(50.dp)
                    .height(10.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
        
        if (isFromMe) {
            ShimmerCircle(
                size = 32.dp,
                modifier = Modifier.padding(start = Spacing.Small)
            )
        }
    }
}
