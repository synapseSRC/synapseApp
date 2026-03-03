package com.synapse.social.studioasinc.ui.notifications

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.runtime.Immutable
import com.synapse.social.studioasinc.ui.components.CircularAvatar

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class StringResource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args.toTypedArray())
        }
    }
}

@Immutable
data class UiNotification(
    val id: String,
    val type: String,
    val actorName: UiText,
    val actorAvatar: String?,
    val message: UiText,
    val timestamp: String,
    val isRead: Boolean,
    val targetId: String? = null
)

@Composable
fun NotificationItem(
    notification: UiNotification,
    onNotificationClick: (UiNotification) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val actorNameString = notification.actorName.asString()
    val messageString = notification.message.asString()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = { onNotificationClick(notification) })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularAvatar(
            imageUrl = notification.actorAvatar,
            contentDescription = "Avatar",
            size = 48.dp,
            onClick = { onUserClick(actorNameString) }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = actorNameString,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = { onUserClick(actorNameString) })
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = messageString,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))

             androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
