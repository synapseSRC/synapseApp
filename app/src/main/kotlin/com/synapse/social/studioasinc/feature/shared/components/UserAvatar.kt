package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

@Composable
fun UserAvatar(
    avatarUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    displayName: String? = null,
    shape: Shape = CircleShape,
    initialsTextStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    // The initials sit under the image so an unloaded or failed URL still identifies the user.
    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.let { name -> name?.firstOrNull { it.isLetterOrDigit() } ?: name?.firstOrNull() }?.uppercaseChar()?.toString() ?: "?",
                style = initialsTextStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "User avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
