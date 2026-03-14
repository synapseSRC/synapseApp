package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.ui.components.GenderBadge
import com.synapse.social.studioasinc.ui.components.VerifiedBadge

@Composable
fun PostHeader(
    user: User,
    timestamp: String,
    onUserClick: () -> Unit,
    onOptionsClick: () -> Unit,
    taggedPeople: List<User> = emptyList(),
    feeling: FeelingActivity? = null,
    locationName: String? = null,
    replyToUsername: String? = null,
    onReplyToClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val name = user.displayName ?: user.username ?: "Unknown"
                val handle = user.username
                val showHandle = !handle.isNullOrBlank()

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (user.verify) {
                    VerifiedBadge()
                }

                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            if (showHandle) {
                                append("@$handle · $timestamp")
                            } else {
                                append("· $timestamp")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (feeling != null || taggedPeople.isNotEmpty() || !locationName.isNullOrEmpty()) {
            val annotatedText = buildAnnotatedString {
                if (feeling != null) {
                    append("is ")
                    append(feeling.emoji)
                    append(" feeling ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(feeling.text)
                    }
                }

                if (taggedPeople.isNotEmpty()) {
                    if (feeling == null) {
                        append("\u2014 with ")
                    } else {
                        append(" with ")
                    }

                    if (taggedPeople.size == 1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(taggedPeople[0].displayName ?: taggedPeople[0].username ?: "Unknown")
                        }
                    } else if (taggedPeople.size == 2) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(taggedPeople[0].displayName ?: taggedPeople[0].username ?: "Unknown")
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(taggedPeople[1].displayName ?: taggedPeople[1].username ?: "Unknown")
                        }
                    } else {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(taggedPeople[0].displayName ?: taggedPeople[0].username ?: "Unknown")
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${taggedPeople.size - 1} others")
                        }
                    }
                }

                if (!locationName.isNullOrEmpty()) {
                    if (feeling == null && taggedPeople.isEmpty()) {
                        append("is at ")
                    } else {
                        append(" at ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(locationName)
                    }
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        if (replyToUsername != null) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val replyingToText = androidx.compose.ui.res.stringResource(
                    com.synapse.social.studioasinc.R.string.replying_to,
                    ""
                ).replace("%s", "").trim()

                Text(
                    text = "$replyingToText ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "@$replyToUsername",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = if (onReplyToClick != null) {
                        Modifier.clickable { onReplyToClick() }
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}
