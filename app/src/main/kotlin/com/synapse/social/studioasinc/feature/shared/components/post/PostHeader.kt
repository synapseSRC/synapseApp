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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import com.synapse.social.studioasinc.ui.components.GenderBadge
import com.synapse.social.studioasinc.ui.components.VerifiedBadge
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.post.postdetail.components.ReplyingToBottomSheet
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun PostHeader(
    user: User,
    timestamp: String,
    onUserClick: () -> Unit,
    onOptionsClick: () -> Unit,
    taggedPeople: List<User> = emptyList(),
    feeling: FeelingActivity? = null,
    locationName: String? = null,
    replyToUsernames: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showReplyingToSheet by remember { mutableStateOf(false) }
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

                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
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
                modifier = Modifier.size(Sizes.IconLarge)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                    modifier = Modifier.size(Sizes.IconSemiSmall),
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
                modifier = Modifier.padding(top = Spacing.Tiny)
            )
        }

        if (replyToUsernames.isNotEmpty()) {
            val visibleCount = 2
            val visible = replyToUsernames.take(visibleCount)
            val overflow = replyToUsernames.size - visibleCount

            val annotated = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(stringResource(R.string.replying_to, "").trimEnd())
                    append(" ")
                }
                visible.forEachIndexed { i, username ->
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("@$username")
                    }
                    if (i < visible.size - 1) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(", ")
                        }
                    }
                }
                if (overflow > 0) {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(" ")
                        append(stringResource(R.string.and_n_more, overflow))
                    }
                }
            }

            Text(
                text = annotated,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (replyToUsernames.size > visibleCount) {
                    Modifier.clickable { showReplyingToSheet = true }
                } else {
                    Modifier
                }
            )
        }

        if (showReplyingToSheet) {
            ReplyingToBottomSheet(
                usernames = replyToUsernames,
                onDismiss = { showReplyingToSheet = false }
            )
        }
    }
}
