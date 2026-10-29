package com.synapse.social.studioasinc.ui.notifications

import com.synapse.social.studioasinc.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.synapse.social.studioasinc.ui.home.FeedLoading
import com.synapse.social.studioasinc.feature.auth.ui.components.ErrorCard
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.GroupedListSpacing
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.ui.components.SectionHeader
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNotificationClick: (UiNotification) -> Unit,
    onUserClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(Spacing.space0)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.startRealtime()
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.stopRealtime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Same page backdrop as Settings: the cardBackground rows are only one tone
            // step away from the app surface, so on the Home scaffold they vanish.
            .background(AppColors.screenBackground)
    ) {
        ErrorCard(
            error = uiState.error?.asString(),
            onDismiss = viewModel::clearError
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refresh() },
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppSpacing.screenPadding),
                        contentPadding = contentPadding
                    ) {
                        groupedNotifications.forEach { (date, notifications) ->
                            item(key = "header_${date.hashCode()}") {
                                SectionHeader(title = date.asString())
                            }
                            itemsIndexed(
                                items = notifications,
                                key = { index, notification -> "${notification.id}_$index" }
                            ) { index, notification ->
                                NotificationItem(
                                    notification = notification,
                                    position = GroupedItemPosition.at(index, notifications.size),
                                    onNotificationClick = handleNotificationClick,
                                    onUserClick = handleUserClick,
                                    modifier = if (index == 0) {
                                        Modifier
                                    } else {
                                        Modifier.padding(top = GroupedListSpacing)
                                    }
                                )
                            }
                            item(key = "gap_${date.hashCode()}") {
                                Spacer(modifier = Modifier.height(AppSpacing.sectionSpacing))
                            }
                        }
                    }
                }
            }
        }
    }
}
