package com.synapse.social.studioasinc.shared.domain.usecase.notification

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import com.synapse.social.studioasinc.shared.domain.model.NotificationError
import io.github.aakira.napier.Napier

class MarkNotificationAsReadUseCase(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            return Result.failure(NotificationError.Unauthorized)
        }

        return try {
            notificationRepository.markAsRead(userId, notificationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to mark notification as read", e)
            Result.failure(NotificationError.NetworkError)
        }
    }
}
