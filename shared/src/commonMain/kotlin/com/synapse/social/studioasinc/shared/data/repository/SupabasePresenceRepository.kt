package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabasePresenceRepository(
    private val client: SupabaseClientLib = SupabaseClient.client,
    private val dispatcher: CoroutineDispatcher = AppDispatchers.IO,
    private val sessionUserIdProvider: (suspend () -> String?)? = null
) : PresenceRepository {

    private val trackingScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + dispatcher)
    private var heartbeatJob: Job? = null
    private var activeChatId: String? = null
    private var heartbeatTickCount = 0
    private val presenceChannel by lazy { client.realtime.channel("presence") }

    private suspend fun currentSessionUserId(): String? {
        return try {
            val session = client.auth.currentSessionOrNull()
            session?.user?.id?.toString()
        } catch (e: Exception) {
            Napier.w("⚠️ Unable to read auth session: ${e.message}", e)
            null
        }
    }

    private suspend fun resolveCurrentUserId(): String? =
        sessionUserIdProvider?.invoke() ?: currentSessionUserId()

    override suspend fun updatePresence(isOnline: Boolean, currentChatId: String?): Result<Unit> {
        val userId = resolveCurrentUserId()
        if (userId == null) {
            Napier.w("Cannot update presence: No active session or user ID")
            return Result.failure(Exception("Not authenticated"))
        }

        if (!isOnline) {
            activeChatId = null
        } else if (currentChatId != null) {
            activeChatId = currentChatId
        }
        val chatIdToSave = if (isOnline) (currentChatId ?: activeChatId) else null

        return try {
            withContext(dispatcher) {
                val now = Clock.System.now().toString()
                val statusStr = if (isOnline) "online" else "offline"
                client.postgrest.from("user_presence").upsert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("status", statusStr)
                        put("last_seen", now)
                        put("updated_at", now)
                        put("current_chat_id", chatIdToSave)
                    }
                )
                Napier.d("✅ Presence updated: userId=$userId, isOnline=$isOnline, status=$statusStr, currentChatId=$chatIdToSave")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("❌ Failed to update presence: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun startPresenceTracking() {
        if (heartbeatJob?.isActive == true) {
            Napier.d("✅ Presence tracking already active")
            return
        }

        val userId = resolveCurrentUserId()
        if (userId == null) {
            Napier.w("⚠️ Cannot start presence tracking: No active session")
            return
        }

        Napier.d("🟢 Starting presence tracking for user: $userId")

        updatePresence(true).onSuccess {
            Napier.d("✅ Initial presence set to online")
        }.onFailure {
            Napier.w("⚠️ Initial presence update failed, heartbeat will retry", it)
        }

        try {
            presenceChannel.subscribe(blockUntilSubscribed = true)
            presenceChannel.track(buildJsonObject {
                put("user_id", userId)
                put("online", true)
            })
            Napier.d("✅ Subscribed to presence channel")
        } catch (e: Exception) {
            Napier.w("⚠️ Failed to subscribe to presence channel", e)
        }

        heartbeatJob?.cancel()
        heartbeatJob = trackingScope.launch {
            while (isActive) {
                delay(30_000)
                if (!isActive) break
                heartbeatTickCount++
                updatePresence(true).onSuccess {
                    Napier.d("💓 Heartbeat #$heartbeatTickCount: Presence updated", tag = "PresenceHeartbeat")
                }.onFailure {
                    Napier.w("⚠️ Heartbeat #$heartbeatTickCount: Update failed, will retry next cycle", it, tag = "PresenceHeartbeat")
                }
            }
            Napier.d("🛑 Presence heartbeat loop exited (tickCount=$heartbeatTickCount, isActive=$isActive)", tag = "PresenceHeartbeat")
        }
        Napier.d("✅ Presence heartbeat started (30s interval)")
    }

    override suspend fun stopPresenceTracking() {
        Napier.d("Stopping presence tracking")
        heartbeatJob?.cancel()
        heartbeatJob = null
        activeChatId = null
        if (resolveCurrentUserId() != null) {
            updatePresence(false).onFailure {
                Napier.w("Failed to set presence offline on stop", it)
            }
        }
        try {
            presenceChannel.unsubscribe()
        } catch (e: Exception) {
            Napier.w("Failed to unsubscribe from presence channel", e)
        }
    }

    override fun observeUserPresence(userId: String): Flow<Boolean> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                val response = try {
                    client.postgrest.from("user_presence")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeSingleOrNull<UserPresenceDto>()
                } catch (e: Exception) {
                    Napier.w("⚠️ Error fetching presence for user $userId: ${e.message}", e)
                    null
                }

                val isActive = if (response == null) {
                    false
                } else {
                    val isStatusOnline = response.status == null || response.status.lowercase() == "online"
                    isStatusOnline && isWithinActiveWindow(response.lastSeen, windowMinutes = 2)
                }
                emit(isActive)
                delay(10_000)
            }
        }
    }

    override suspend fun isUserInChat(userId: String, chatId: String): Boolean {
        val response = try {
            client.postgrest.from("user_presence")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserPresenceDto>()
        } catch (e: Exception) {
            Napier.w("⚠️ Error checking if user $userId is in chat $chatId: ${e.message}", e)
            return false
        }

        if (response == null) return false

        val isStatusOnline = response.status == null || response.status.lowercase() == "online"
        val isOnline = isStatusOnline && isWithinActiveWindow(response.lastSeen, windowMinutes = 2)
        return response.currentChatId == chatId && isOnline
    }

    fun isWithinActiveWindow(lastSeen: String?, windowMinutes: Long = 5): Boolean {
        if (lastSeen == null) return false
        return try {
            val lastSeenInstant = kotlinx.datetime.Instant.parse(lastSeen)
            val now = Clock.System.now()
            val diff = now - lastSeenInstant
            diff.inWholeMinutes < windowMinutes
        } catch (e: Exception) {
            Napier.w("⚠️ Failed to parse last_seen timestamp: $lastSeen", e)
            false
        }
    }
}

@Serializable
data class UserPresenceDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("current_chat_id") val currentChatId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
