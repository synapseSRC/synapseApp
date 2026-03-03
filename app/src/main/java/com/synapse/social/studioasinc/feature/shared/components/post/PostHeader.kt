package com.synapse.social.studioasinc.feature.shared.components.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        CircularAvatar(
            imageUrl = user.avatar,
            contentDescription = "Avatar of ${user.username}",
            onClick = onUserClick,
            size = 40.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onUserClick)
        ) {
            val annotatedText = buildAnnotatedString {

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(user.displayName ?: user.username ?: "Unknown")
                }


                if (feeling != null) {
                    append(" is ")
                    append(feeling.emoji)
                    append(" feeling ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(feeling.text)
                    }
                }


                if (taggedPeople.isNotEmpty()) {
                    if (feeling == null) {
                        append(" \u2014 with ")
                    } else {
                        append(" with ")
                    }

                    if (taggedPeople.size == 1) {
                         withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                         }
                    } else if (taggedPeople.size == 2) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[1].displayName ?: taggedPeople[1].username)
                        }
                    } else {
                         withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append("${taggedPeople.size - 1} others")
                        }
                    }
                }


                if (!locationName.isNullOrEmpty()) {
                    if (feeling == null && taggedPeople.isEmpty()) {
                        append(" is at ")
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )









            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.padding(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options"
            )
        }
    }
}
