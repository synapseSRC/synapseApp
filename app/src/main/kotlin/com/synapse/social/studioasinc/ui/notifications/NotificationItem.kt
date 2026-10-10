package com.synapse.social.studioasinc.ui.notifications

import com.synapse.social.studioasinc.R

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.core.util.UiText
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.NotificationTarget
import com.synapse.social.studioasinc.ui.components.CircularAvatar

@Immutable
data class UiNotification(
    val id: String,
    val type: String,
    val actorId: String?,
    val actorName: UiText,
    val actorAvatar: String?,
    val message: UiText,
    val timestamp: String,
    val isRead: Boolean,
    val targetId: String? = null,
    val target: NotificationTarget = NotificationTarget.Unknown
)

enum class NotificationGroupPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST
}

@Composable
fun getNotificationCardShape(position: NotificationGroupPosition): androidx.compose.ui.graphics.Shape {
    val outer = Sizes.CornerLarge
    val inner = Spacing.ExtraSmall
    return when (position) {
        NotificationGroupPosition.SINGLE -> MaterialTheme.shapes.large
        NotificationGroupPosition.FIRST -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner
        )
        NotificationGroupPosition.MIDDLE -> RoundedCornerShape(inner)
        NotificationGroupPosition.LAST -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer
        )
    }
}

@Composable
fun NotificationItem(
    notification: UiNotification,
    position: NotificationGroupPosition,
    onNotificationClick: (UiNotification) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actorNameString = notification.actorName.asString()
    val messageString = notification.message.asString()
    val shape = getNotificationCardShape(position)

    val backgroundColor = if (!notification.isRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
            .clip(shape)
            .background(backgroundColor)
            .clickable { onNotificationClick(notification) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularAvatar(
                imageUrl = notification.actorAvatar,
                contentDescription = "Avatar",
                size = Sizes.IconGiant,
                onClick = {
                    notification.actorId?.let { onUserClick(it) }
                }
            )

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actorNameString,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable {
                            notification.actorId?.let { onUserClick(it) }
                        }
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text(
                        text = messageString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = notification.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(Spacing.Small))

                Box(
                    modifier = Modifier
                        .size(Spacing.Small)
                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}
