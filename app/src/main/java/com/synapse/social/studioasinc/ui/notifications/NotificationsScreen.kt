package com.synapse.social.studioasinc.ui.notifications

import androidx.compose.foundation.background
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.synapse.social.studioasinc.ui.components.ExpressivePullToRefreshIndicator
import com.synapse.social.studioasinc.ui.home.FeedLoading
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
    val pullToRefreshState = rememberPullToRefreshState()
    

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

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            state = pullToRefreshState,
            indicator = {
                ExpressivePullToRefreshIndicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            val showLoading = uiState.isLoading && uiState.notifications.isEmpty()
            val showEmpty = !uiState.isLoading && uiState.notifications.isEmpty()

            if (showLoading) {
                FeedLoading()
            } else if (showEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_notifications), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val groupedNotifications = uiState.groupedNotifications

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    groupedNotifications.forEach { (date, notifications) ->
                        item {
                            NotificationHeader(date.asString())
                        }
                        itemsIndexed(notifications, key = { index, it -> "${it.id}_${index}" }) { index, notification ->
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
