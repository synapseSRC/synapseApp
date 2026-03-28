package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopAppBar(
    title: String = "Inbox",
    avatarUrl: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    InboxLargeTopAppBar(
        title = title,
        avatarUrl = avatarUrl,
        scrollBehavior = scrollBehavior,
        onProfileClick = onProfileClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxLargeTopAppBar(
    title: String,
    avatarUrl: String?,
    scrollBehavior: TopAppBarScrollBehavior?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LargeTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = Spacing.Small)
            )
        },
        actions = {

             Box(
                 modifier = Modifier
                     .size(Sizes.IconMassive)
                     .clip(CircleShape)
                     .background(MaterialTheme.colorScheme.primary)
                     .clickable(onClick = onProfileClick),
                 contentAlignment = Alignment.Center
             ) {
                 if (avatarUrl != null) {
                     AsyncImage(
                         model = avatarUrl,
                         contentDescription = "Profile",
                         modifier = Modifier.fillMaxSize(),
                         contentScale = ContentScale.Crop
                     )
                 } else {
                     Text(
                         text = stringResource(R.string.default_avatar_letter),
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onPrimary
                     )
                 }
             }

             Spacer(modifier = Modifier.width(Spacing.Medium))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}
