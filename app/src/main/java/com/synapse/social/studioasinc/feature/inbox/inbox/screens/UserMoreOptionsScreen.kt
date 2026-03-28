package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.widget.Toast
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.UserMoreOptionsViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.util.TimestampFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserMoreOptionsScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: UserMoreOptionsViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    var moreOptionsExpanded by remember { mutableStateOf(false) }

    // Scaffold matching the dark theme Telegram look using Material 3 Expressive
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { moreOptionsExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = moreOptionsExpanded,
                            onDismissRequest = { moreOptionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Search") },
                                onClick = {
                                    moreOptionsExpanded = false
                                    Toast.makeText(context, "Search not yet supported", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    moreOptionsExpanded = false
                                    Toast.makeText(context, "Share not yet supported", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            AsyncImage(
                model = userProfile?.avatar,
                contentDescription = "Profile Avatar",
                modifier = Modifier
                    .size(Sizes.EmptyStateIconSmall)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Filled.Person),
                error = rememberVectorPainter(Icons.Filled.Person)
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Name
            Text(
                text = userProfile?.displayName ?: userProfile?.username ?: "...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Last seen
            Text(
                text = userProfile?.lastSeen?.let { "last seen ${TimestampFormatter.formatRelative(it)}" } ?: "last seen recently",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                ActionButton(
                    icon = Icons.Default.ChatBubble,
                    label = "Message",
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateBack
                )
                ActionButton(
                    icon = Icons.Default.NotificationsOff,
                    label = "Mute",
                    modifier = Modifier.weight(1f),
                    onClick = { Toast.makeText(context, "Muted", Toast.LENGTH_SHORT).show() }
                )
                ActionButton(
                    icon = Icons.Default.Call,
                    label = "Call",
                    modifier = Modifier.weight(1f),
                    onClick = { Toast.makeText(context, "Calling not yet supported", Toast.LENGTH_SHORT).show() }
                )
                ActionButton(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    modifier = Modifier.weight(1f),
                    onClick = { Toast.makeText(context, "Video calls not yet supported", Toast.LENGTH_SHORT).show() }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Large))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Sizes.CornerLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium)
                ) {
                    InfoItem(
                        title = userProfile?.email ?: "Not available",
                        subtitle = "Email"
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    InfoItem(
                        title = userProfile?.bio ?: "No bio available",
                        subtitle = "Bio"
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    InfoItem(
                        title = userProfile?.birthday ?: "Not specified",
                        subtitle = "Birthday"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs for Posts, Media, Files, GIFs
            var selectedTabIndex by remember { mutableIntStateOf(1) }
            val tabs = listOf("Posts", "Media", "Files", "GIFs")

            // Replicating segmented buttons style tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { selectedTabIndex = index }
                            .background(if (selectedTabIndex == index) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid for media
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(6) { index ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        // Image Placeholder
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoItem(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
