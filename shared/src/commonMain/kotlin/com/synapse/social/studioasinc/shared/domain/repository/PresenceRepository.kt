package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.presence.UserPresence
import kotlinx.coroutines.flow.Flow

interface PresenceRepository {
    suspend fun updatePresence(isOnline: Boolean, currentChatId: String? = null): Result<Unit>
    suspend fun startPresenceTracking()
    suspend fun stopPresenceTracking()
    fun observeUserPresence(userId: String): Flow<Boolean>
    fun observeUserPresenceDetail(userId: String): Flow<UserPresence>
    fun isWithinActiveWindow(lastSeen: String?, windowMinutes: Long = 2): Boolean
    suspend fun isUserInChat(userId: String, chatId: String): Boolean
}
