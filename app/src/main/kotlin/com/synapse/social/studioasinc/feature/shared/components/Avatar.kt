package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun CircularAvatar(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.AvatarDefault,
    displayName: String? = null,
    onClick: (() -> Unit)? = null
) {
    // The initials sit under the picture so a profile image that is missing, expired or otherwise
    // unloadable still identifies the user instead of leaving an empty circle — Google account
    // photos are remote URLs that can stop resolving.
    val initial = displayName
        ?.let { name -> name.firstOrNull { it.isLetterOrDigit() } ?: name.firstOrNull() }
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
