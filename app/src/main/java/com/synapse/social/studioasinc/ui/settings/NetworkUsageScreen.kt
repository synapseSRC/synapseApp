package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkUsageScreen(
    viewModel: NetworkUsageViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val usageItems by viewModel.usageItems.collectAsState()
    val totalSent by viewModel.totalSent.collectAsState()
    val totalReceived by viewModel.totalReceived.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = stringResource(R.string.network_usage_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = Spacing.Medium, top = Spacing.Medium, start = Spacing.Medium, end = Spacing.Medium)
            ) {
                item {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.Medium)
                            .padding(Spacing.Medium)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.shapes.large
                            )
                            .padding(Spacing.Medium)
                    ) {
                        Text(
                            text = stringResource(R.string.network_usage_heading),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f).padding(end = Spacing.Medium)) {
                                Text(
                                    text = stringResource(R.string.network_usage_sent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatBytes(totalSent),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.network_usage_received),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatBytes(totalReceived),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))
                }

                items(usageItems) { item ->
                    ListItem(
                        headlineContent = { Text(text = item.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_cloud_upload),
                                    contentDescription = stringResource(R.string.cd_upload_icon),
                                    modifier = Modifier.size(Spacing.SmallMedium),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                                Text(text = formatBytes(item.sentBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(Spacing.Medium))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download),
                                    contentDescription = stringResource(R.string.cd_download_icon),
                                    modifier = Modifier.size(Spacing.SmallMedium),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                                Text(text = formatBytes(item.receivedBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(
                                    id = when(item.label) {
                                        "Calls" -> R.drawable.ic_call
                                        "Media" -> R.drawable.ic_image
                                        "Google Drive" -> R.drawable.ic_cloud_upload
                                        "Messages" -> R.drawable.ic_message
                                        "Status" -> R.drawable.ic_photo_library
                                        else -> R.drawable.ic_network_check
                                    }
                                ),
                                contentDescription = stringResource(R.string.cd_category_icon),
                                modifier = Modifier.size(Spacing.Large),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                item {

                     Box(modifier = Modifier.fillMaxWidth().padding(Spacing.Medium)) {
                        TextButton(
                            onClick = {  },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(text = stringResource(R.string.network_reset_statistics), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                     }
                }
            }
        }
    }
}
