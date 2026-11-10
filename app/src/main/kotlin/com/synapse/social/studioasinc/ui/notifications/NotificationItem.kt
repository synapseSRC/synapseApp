package com.synapse.social.studioasinc.ui.notifications

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

/** The actor name and the description that follows it, ready to render as one inline text run. */
internal data class NotificationTextParts(
    val actorName: String,
    val description: String
)

/**
 * Splits a notification into the actor name and the description that follows it.
 *
 * The backend exposes the actor's display name twice: once in the structured actor object and,
 * for several notification types (likes, new posts, follows and replies), already prefixed inside
 * the message body - e.g. body "Work liked your post." for actor "Work". Rendering the structured
 * name *and* the body verbatim therefore repeated the name ("Work Work liked your post."). The
 * leading duplicate is dropped here so the name is rendered exactly once, from a single source,
 * instead of deleting it from the data model.
 */
internal fun notificationTextParts(actorName: String, message: String): NotificationTextParts {
    val name = actorName.trim()
    val body = message.trim()
    if (name.isEmpty() || body.isEmpty()) return NotificationTextParts(name, body)
    if (!body.startsWith(name, ignoreCase = true)) return NotificationTextParts(name, body)

    // Only strip when the name ends on a word boundary, so a name that merely prefixes a longer
    // word stays intact (actor "Sam" must not turn "Sample post" into "ple post").
    val boundary = body.getOrNull(name.length)
    if (boundary != null && boundary.isLetterOrDigit()) return NotificationTextParts(name, body)

    val remainder = body.substring(name.length)
        .trimStart()
        .trimStart(':', '-', '\u2013', '\u2014')
        .trimStart()
    return NotificationTextParts(name, remainder)
}

/**
 * Builds the card message as a single [AnnotatedString] so the actor name and the description
 * share one text flow and wrap naturally across the full width of the card instead of being
 * squeezed into two side-by-side columns. The name keeps its bold weight and its tap target
 * (opening the actor's profile); the description stays regular weight.
 */
private fun buildNotificationText(
    parts: NotificationTextParts,
    actorId: String?,
    actorNameColor: Color,
    descriptionColor: Color,
    onUserClick: (String) -> Unit
): AnnotatedString = buildAnnotatedString {
    val nameStyle = SpanStyle(color = actorNameColor, fontWeight = FontWeight.Bold)
    if (actorId == null) {
        withStyle(nameStyle) { append(parts.actorName) }
    } else {
        pushLink(
            LinkAnnotation.Clickable(
                tag = "notification-actor",
                styles = TextLinkStyles(style = nameStyle),
                linkInteractionListener = { onUserClick(actorId) }
            )
        )
        append(parts.actorName)
        pop()
    }

    if (parts.description.isNotEmpty()) {
        append(" ")
        withStyle(SpanStyle(color = descriptionColor, fontWeight = FontWeight.Normal)) {
            append(parts.description)
        }
    }
}

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
    val actorId = notification.actorId
    val actorNameColor = AppColors.itemContent
    val descriptionColor = AppColors.supportingText

    val annotatedMessage = remember(
        actorNameString,
        messageString,
        actorId,
        actorNameColor,
        descriptionColor,
        onUserClick
    ) {
        buildNotificationText(
            parts = notificationTextParts(actorNameString, messageString),
            actorId = actorId,
            actorNameColor = actorNameColor,
            descriptionColor = descriptionColor,
            onUserClick = onUserClick
        )
    }

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
                Text(
                    text = annotatedMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
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
