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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle

@Composable
fun FeedLoading() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(5) {
            PostShimmer()
        }
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.SmallMedium, vertical = Spacing.Small)
        ) {
            // Left Column: Avatar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                ShimmerCircle(size = 48.dp)
            }

            Spacer(modifier = Modifier.width(Spacing.SmallMedium))

            // Right Column: Content
            Column(modifier = Modifier.weight(1f)) {
                // Header (Username and Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(modifier = Modifier.width(120.dp).height(16.dp))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    ShimmerBox(modifier = Modifier.width(40.dp).height(16.dp))
                }

                Spacer(modifier = Modifier.height(Spacing.Small))

                // Text Content
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp))
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))

                Spacer(modifier = Modifier.height(Spacing.SmallMedium))

                // Media Area
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp))

                // Interaction Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.Small, bottom = Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        ShimmerBox(modifier = Modifier.width(48.dp).height(24.dp))
                    }
                    ShimmerBox(modifier = Modifier.width(24.dp).height(24.dp))
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }
}
