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

@Immutable
data class NotificationsUiState(
    val notifications: List<UiNotification> = emptyList(),
    val isLoading: Boolean = false,
    val unreadCount: Int = 0
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

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getNotificationsUseCase().collect { result ->
                result.onSuccess { notifications ->
                    val uiNotifications = notifications.map { mapDomainToUi(it) }
                    _uiState.update {
                        it.copy(
                            notifications = uiNotifications,
                            isLoading = false,
                            unreadCount = uiNotifications.count { !it.isRead }
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
                        state.copy(
                            notifications = listOf(newNotification) + state.notifications,
                            unreadCount = state.unreadCount + 1
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
                unreadCount = newUnreadCount
            )
        }
    }
}
