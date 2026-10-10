package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.inbox.inbox.models.CallDirection
import com.synapse.social.studioasinc.feature.inbox.inbox.models.CallRecord
import com.synapse.social.studioasinc.feature.inbox.inbox.models.CallType
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlinx.datetime.Instant

@Composable
fun CallRecordItem(
    callRecord: CallRecord,
    onCallClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCallClick(callRecord.userId, callRecord.isVideo) }
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = callRecord.userAvatar,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(Sizes.AvatarMedium)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = callRecord.userName,
                style = MaterialTheme.typography.titleMedium,
                color = if (callRecord.direction == CallDirection.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (callRecord.direction) {
                        CallDirection.INCOMING -> Icons.Default.CallReceived
                        CallDirection.OUTGOING -> Icons.Default.CallMade
                        CallDirection.MISSED -> Icons.Default.CallMissed
                    },
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.IconSmall),
                    tint = if (callRecord.direction == CallDirection.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                Text(
                    text = TimestampFormatter.formatRelative(Instant.fromEpochMilliseconds(callRecord.timestamp).toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = { onCallClick(callRecord.userId, callRecord.isVideo) }) {
            Icon(
                imageVector = if (callRecord.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Call Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
