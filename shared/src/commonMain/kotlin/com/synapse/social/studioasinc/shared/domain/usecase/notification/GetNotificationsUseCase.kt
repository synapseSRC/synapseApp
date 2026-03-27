package com.synapse.social.studioasinc.shared.domain.usecase.notification

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.model.Notification
import com.synapse.social.studioasinc.shared.domain.model.NotificationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.github.aakira.napier.Napier

class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Result<List<Notification>>> = flow {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            emit(Result.failure(NotificationError.Unauthorized))
            return@flow
        }

        try {
            val notifications = notificationRepository.fetchNotifications(userId)
            emit(Result.success(notifications))
        } catch (e: Exception) {
            Napier.e("Failed to get notifications", e)
            emit(Result.failure(NotificationError.NetworkError))
        }
    }
}
