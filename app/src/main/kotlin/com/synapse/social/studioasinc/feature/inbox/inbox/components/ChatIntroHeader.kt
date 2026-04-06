package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User

@Composable
fun ChatIntroHeader(
    participantProfile: User?,
    initialParticipantName: String?,
    avatarUrl: String?,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.ExtraLarge, horizontal = Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = stringResource(id = R.string.cd_profile_picture),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Text(
            text = participantProfile?.displayName ?: participantProfile?.name ?: participantProfile?.username ?: initialParticipantName ?: "",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

        val subtitle = if (!participantProfile?.bio.isNullOrBlank()) {
            participantProfile?.bio ?: ""
        } else {
            val username = participantProfile?.username ?: initialParticipantName?.replace(" ", "")?.lowercase() ?: ""
            val followers = participantProfile?.followersCount ?: 0
            if (username.isNotEmpty()) {
                "@$username · $followers followers"
            } else {
                "$followers followers"
            }
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.MediumLarge))

        OutlinedButton(onClick = onViewProfile) {
            Text(text = stringResource(id = R.string.view_profile))
        }

        Spacer(modifier = Modifier.height(Spacing.Large))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(
                text = stringResource(id = R.string.messages_are_end_to_end_encrypted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Trigger CI
// Trigger CI 2
