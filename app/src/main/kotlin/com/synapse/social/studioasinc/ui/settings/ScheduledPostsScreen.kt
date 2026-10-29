package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.feature.shared.theme.Spacing

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.CreatePostActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import com.synapse.social.studioasinc.ui.createpost.ScheduleDateTimePickerDialog
import kotlinx.datetime.Instant
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledPostsScreen(
    viewModel: ScheduledPostsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var reschedulingPostId by remember { mutableStateOf<String?>(null) }
    var cancelConfirmPostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_scheduled_posts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadScheduledPosts() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.posts.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null && uiState.posts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.space24),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.something_went_wrong),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(Spacing.space16))
                        Button(onClick = { viewModel.loadScheduledPosts() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                uiState.posts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.space32),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Spacing.space16))
                        Text(
                            text = stringResource(R.string.scheduled_posts_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Spacing.space8))
                        Text(
                            text = stringResource(R.string.scheduled_posts_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.space16)
                        )
                        Spacer(modifier = Modifier.height(Spacing.space24))
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, CreatePostActivity::class.java))
                            }
                        ) {
                            Text(stringResource(R.string.schedule_post_action))
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.space16),
                        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
                    ) {
                        items(uiState.posts, key = { it.id }) { scheduledPost ->
                            ScheduledPostCard(
                                scheduledPost = scheduledPost,
                                onEdit = {
                                    val intent = Intent(context, CreatePostActivity::class.java).apply {
                                        putExtra("scheduled_post_id", scheduledPost.id)
                                    }
                                    context.startActivity(intent)
                                },
                                onReschedule = { reschedulingPostId = scheduledPost.id },
                                onRetry = { viewModel.retryScheduledPost(scheduledPost.id) },
                                onCancel = { cancelConfirmPostId = scheduledPost.id }
                            )
                        }
                    }
                }
            }
        }
    }

    reschedulingPostId?.let { postId ->
        val currentPost = uiState.posts.find { it.id == postId }
        ScheduleDateTimePickerDialog(
            initialScheduledAt = currentPost?.scheduledAt,
            onDismiss = { reschedulingPostId = null },
            onScheduleSet = { newTime ->
                if (newTime != null) {
                    viewModel.reschedulePost(postId, newTime)
                }
                reschedulingPostId = null
            }
        )
    }

    cancelConfirmPostId?.let { postId ->
        AlertDialog(
            onDismissRequest = { cancelConfirmPostId = null },
            title = { Text(stringResource(R.string.cancel_scheduled_post)) },
            text = { Text("Are you sure you want to cancel and delete this scheduled post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelScheduledPost(postId)
                        cancelConfirmPostId = null
                    }
                ) {
                    Text(stringResource(R.string.m_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelConfirmPostId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ScheduledPostCard(
    scheduledPost: ScheduledPost,
    onEdit: () -> Unit,
    onReschedule: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.cardShape,
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.space16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusBadge(status = scheduledPost.status)

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_post)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reschedule_post_action)) },
                            onClick = {
                                menuExpanded = false
                                onReschedule()
                            },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) }
                        )
                        if (scheduledPost.status == ScheduledPost.STATUS_FAILED) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.retry)) },
                                onClick = {
                                    menuExpanded = false
                                    onRetry()
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cancel_scheduled_post)) },
                            onClick = {
                                menuExpanded = false
                                onCancel()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.space8))

            // Post content preview
            val text = scheduledPost.postRequest.postText
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.space8))
            }

            if (scheduledPost.postRequest.mediaItems.isNotEmpty()) {
                Text(
                    text = "📷 ${scheduledPost.postRequest.mediaItems.size} media item(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(Spacing.space4))
            }

            scheduledPost.postRequest.pollQuestion?.let { pollQ ->
                Text(
                    text = "📊 Poll: $pollQ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(Spacing.space4))
            }

            scheduledPost.errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(Spacing.space8))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: $err",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.space8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.space12))

            // Scheduled time label
            val formattedTime = remember(scheduledPost.scheduledAt) {
                try {
                    val instant = Instant.parse(scheduledPost.scheduledAt)
                    val date = java.util.Date(instant.toEpochMilliseconds())
                    java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault()).format(date)
                } catch (e: Exception) {
                    scheduledPost.scheduledAt
                }
            }

            Text(
                text = stringResource(R.string.scheduled_time_label, formattedTime),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status) {
        ScheduledPost.STATUS_PENDING -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "SCHEDULED")
        ScheduledPost.STATUS_PUBLISHED -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "PUBLISHED")
        ScheduledPost.STATUS_FAILED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "FAILED")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurface, status.uppercase())
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4)
        )
    }
}
