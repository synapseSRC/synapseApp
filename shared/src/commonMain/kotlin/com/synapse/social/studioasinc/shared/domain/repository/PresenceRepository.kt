package com.synapse.social.studioasinc.shared.domain.repository

import kotlinx.coroutines.flow.Flow

interface PresenceRepository {
    suspend fun updatePresence(isOnline: Boolean, currentChatId: String? = null): Result<Unit>
    suspend fun startPresenceTracking()
    suspend fun stopPresenceTracking()
    fun observeUserPresence(userId: String): Flow<Boolean>
    suspend fun isUserInChat(userId: String, chatId: String): Boolean
}
