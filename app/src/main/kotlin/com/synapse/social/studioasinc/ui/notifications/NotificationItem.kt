package com.synapse.social.studioasinc.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.core.util.UiText
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.NotificationTarget
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

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

@Composable
fun NotificationItem(
    notification: UiNotification,
    position: GroupedItemPosition,
    onNotificationClick: (UiNotification) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actorNameString = notification.actorName.asString()
    val messageString = notification.message.asString()

    Surface(
        onClick = { onNotificationClick(notification) },
        modifier = modifier.fillMaxWidth(),
        shape = position.shape,
        color = AppColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.rowHorizontalPadding,
                    vertical = AppSpacing.itemVerticalPadding
                ),
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

            Spacer(modifier = Modifier.width(Spacing.space16))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actorNameString,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.itemContent,
                        modifier = Modifier.clickable {
                            notification.actorId?.let { onUserClick(it) }
                        }
                    )
                    Spacer(modifier = Modifier.width(Spacing.space4))
                    Text(
                        text = messageString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.supportingText
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.space4))
                Text(
                    text = notification.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.supportingText
                )
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(Spacing.space8))

                Box(
                    modifier = Modifier
                        .size(Sizes.StatusDotSmall)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
