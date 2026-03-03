package com.synapse.social.studioasinc.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.synapse.social.studioasinc.ui.home.FeedLoading
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

private fun formatDateHeader(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val today = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        when (localDateTime.date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
                "$month ${localDateTime.dayOfMonth}, ${localDateTime.year}"
            }
        }
    } catch (e: Exception) {
        timestamp.split("T").firstOrNull() ?: "Other"
    }
}

@Composable
fun NotificationHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNotificationClick: (UiNotification) -> Unit,
    onUserClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isLoading)


    val currentOnNotificationClick by rememberUpdatedState(onNotificationClick)
    val currentOnUserClick by rememberUpdatedState(onUserClick)

    val handleNotificationClick = remember(viewModel) {
        { notification: UiNotification ->
            viewModel.markAsRead(notification.id)
            currentOnNotificationClick(notification)
        }
    }

    val handleUserClick = remember {
        { userId: String ->
            currentOnUserClick(userId)
        }
    }

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.refresh() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading && uiState.notifications.isEmpty()) {
                FeedLoading()
            } else if (uiState.notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notifications", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val groupedNotifications = remember(uiState.notifications) {
                    uiState.notifications.groupBy {
                        formatDateHeader(it.timestamp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    groupedNotifications.forEach { (date, notifications) ->
                        item {
                            NotificationHeader(date)
                        }
                        items(notifications, key = { it.id }) { notification ->
                            NotificationItem(
                                notification = notification,
                                onNotificationClick = handleNotificationClick,
                                onUserClick = handleUserClick
                            )
                        }
                    }
                }
            }
        }
    }
}
