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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage



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
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        actions = {

             Box(
                 modifier = Modifier
                     .size(40.dp)
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
                         text = "A",
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onPrimary
                     )
                 }
             }

             Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}
