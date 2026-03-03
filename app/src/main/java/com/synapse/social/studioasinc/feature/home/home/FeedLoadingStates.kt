package com.synapse.social.studioasinc.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle

@Composable
fun FeedLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ExpressiveLoadingIndicator()
    }
}

@Composable
fun FeedError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
fun FeedEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_posts_follow_desc),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun PostShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerCircle(size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                ShimmerBox(modifier = Modifier.width(120.dp).height(16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row {
             ShimmerBox(modifier = Modifier.width(60.dp).height(24.dp))
             Spacer(modifier = Modifier.width(16.dp))
             ShimmerBox(modifier = Modifier.width(60.dp).height(24.dp))
        }
    }
}
