package com.synapse.social.studioasinc.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.TimeUtils
import com.synapse.social.studioasinc.shared.domain.model.Notification
import com.synapse.social.studioasinc.shared.domain.model.NotificationMessageType
import com.synapse.social.studioasinc.shared.domain.usecase.notification.GetNotificationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.MarkNotificationAsReadUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.notification.SubscribeToNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

@Immutable
data class NotificationsUiState(
    val notifications: List<UiNotification> = emptyList(),
    val isLoading: Boolean = false,
    val unreadCount: Int = 0,
    val groupedNotifications: Map<UiText, List<UiNotification>> = emptyMap()
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val subscribeToNotificationsUseCase: SubscribeToNotificationsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var realtimeJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getNotificationsUseCase().collect { result ->
                result.onSuccess { notifications ->
                    val uiNotifications = notifications.map { mapDomainToUi(it) }
                    _uiState.update {
                        it.copy(
                            notifications = uiNotifications,
                            isLoading = false,
                            unreadCount = uiNotifications.count { !it.isRead },
                            groupedNotifications = uiNotifications.groupBy { formatDateHeader(it.timestamp) }
                        )
                    }
                    subscribeToRealtime()
                }.onFailure { error ->
                    Napier.e("Failed to load notifications: $error")
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun subscribeToRealtime() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                subscribeToNotificationsUseCase().collect { notification ->
                    val newNotification = mapDomainToUi(notification)
                    _uiState.update { state ->
                        val newNotifications = listOf(newNotification) + state.notifications
                        state.copy(
                            notifications = newNotifications,
                            unreadCount = state.unreadCount + 1,
                            groupedNotifications = newNotifications.groupBy { formatDateHeader(it.timestamp) }
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e("Realtime error", e)
            }
        }
    }

    private fun mapDomainToUi(notification: Notification): UiNotification {
        val message = when (notification.messageType) {
            NotificationMessageType.CUSTOM -> notification.message?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.notification_fallback_message)
            NotificationMessageType.FALLBACK -> UiText.StringResource(R.string.notification_fallback_message)
        }
        val actorName = notification.actorName?.let { UiText.DynamicString(it) } ?: UiText.StringResource(R.string.notification_new_activity)

        return UiNotification(
            id = notification.id,
            type = notification.type,
            actorId = notification.actorId,
            actorName = actorName,
            actorAvatar = notification.actorAvatar,
            message = message,
            timestamp = TimeUtils.getTimeAgo(notification.timestamp),
            isRead = notification.isRead,
            targetId = notification.targetId
        )
    }

    fun refresh() {
        loadNotifications()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            // Optimistic update
            setNotificationReadState(notificationId, isRead = true)

            markNotificationAsReadUseCase(notificationId).onFailure { e ->
                Napier.e("Failed to mark as read for notification $notificationId", e)
                // Revert optimistic update
                setNotificationReadState(notificationId, isRead = false)
            }
        }
    }

    private fun setNotificationReadState(notificationId: String, isRead: Boolean) {
        _uiState.update { state ->
            val index = state.notifications.indexOfFirst { it.id == notificationId }
            if (index == -1 || state.notifications[index].isRead == isRead) return@update state

            val updatedList = state.notifications.toMutableList()
            updatedList[index] = updatedList[index].copy(isRead = isRead)

            val newUnreadCount = if (isRead) {
                (state.unreadCount - 1).coerceAtLeast(0)
            } else {
                state.unreadCount + 1
            }

            state.copy(
                notifications = updatedList,
                unreadCount = newUnreadCount,
                groupedNotifications = updatedList.groupBy { formatDateHeader(it.timestamp) }
            )
        }
    }

    private fun formatDateHeader(timestamp: String): UiText {
        return try {
            val instant = Instant.parse(timestamp)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val today = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
            val yesterday = today.minus(1, DateTimeUnit.DAY)

            when (localDateTime.date) {
                today -> UiText.StringResource(R.string.notification_today)
                yesterday -> UiText.StringResource(R.string.notification_yesterday)
                else -> {
                    val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    UiText.DynamicString("$month ${localDateTime.dayOfMonth}, ${localDateTime.year}")
                }
            }
        } catch (e: Exception) {
            UiText.DynamicString(timestamp.split("T").firstOrNull() ?: "Other")
        }
    }
}
