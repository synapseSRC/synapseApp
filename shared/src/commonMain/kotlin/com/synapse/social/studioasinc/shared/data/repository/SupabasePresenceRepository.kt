package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.repository.PresenceRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabasePresenceRepository(
    private val client: SupabaseClientLib = SupabaseClient.client
) : PresenceRepository {
    
    private var heartbeatJob: Job? = null
    private val presenceChannel by lazy { client.realtime.channel("presence") }
    
    override suspend fun updatePresence(isOnline: Boolean, currentChatId: String?): Result<Unit> = runCatching {
        val session = client.auth.currentSessionOrNull() ?: run {
            Napier.w("Cannot update presence: No active session")
            return Result.failure(Exception("Not authenticated"))
        }
        val userId = session.user?.id?.toString() ?: run {
            Napier.w("Cannot update presence: No user ID in session")
            return Result.failure(Exception("No user ID"))
        }
        
        withContext(Dispatchers.IO) {
            try {
                client.postgrest.from("user_presence").upsert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("is_online", isOnline)
                        put("last_seen", Clock.System.now().toString())
                        put("activity_status", if (isOnline) "online" else "offline")
                        put("updated_at", Clock.System.now().toString())
                        put("current_chat_id", currentChatId)
                    }
                )
                Napier.d("✅ Presence updated: userId=$userId, isOnline=$isOnline, status=${if (isOnline) "online" else "offline"}")
            } catch (e: Exception) {
                Napier.e("❌ Failed to update presence: ${e.message}", e)
                throw e
            }
        }
    }
    
    override suspend fun startPresenceTracking() {
        val session = client.auth.currentSessionOrNull() ?: run {
            Napier.w("⚠️ Cannot start presence tracking: No active session")
            return
        }
        val userId = session.user?.id?.toString() ?: run {
            Napier.w("⚠️ Cannot start presence tracking: No user ID")
            return
        }
        
        Napier.d("🟢 Starting presence tracking for user: $userId")
        
        // Initial presence update
        updatePresence(true).onSuccess {
            Napier.d("✅ Initial presence set to online")
        }.onFailure { 
            Napier.e("❌ Failed initial presence update", it)
            return // Don't start heartbeat if initial update fails
        }
        
        // Subscribe to realtime channel
        try {
            presenceChannel.subscribe(blockUntilSubscribed = true)
            presenceChannel.track(buildJsonObject {
                put("user_id", userId)
                put("online", true)
            })
            Napier.d("✅ Subscribed to presence channel")
        } catch (e: Exception) {
            Napier.e("❌ Failed to subscribe to presence channel", e)
        }
        
        // Start heartbeat
        heartbeatJob?.cancel()
        heartbeatJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(30_000) // 30s heartbeat
                updatePresence(true).onSuccess {
                    Napier.d("💓 Heartbeat: Presence updated")
                }.onFailure { 
                    Napier.e("❌ Heartbeat update failed", it)
                }
            }
        }
        Napier.d("✅ Presence heartbeat started (30s interval)")
    }
    
    override suspend fun stopPresenceTracking() {
        Napier.d("Stopping presence tracking")
        heartbeatJob?.cancel()
        updatePresence(false).onFailure {
            Napier.e("Failed to update presence on stop", it)
        }
        try {
            presenceChannel.unsubscribe()
        } catch (e: Exception) {
            Napier.e("Failed to unsubscribe from presence channel", e)
        }
    }
    
    override fun observeUserPresence(userId: String): Flow<Boolean> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                val isActive = runCatching {
                    val response = client.postgrest.from("user_presence")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                        .decodeSingle<UserPresenceDto>()
                    
                    // User is active if online and last_seen within 5 minutes
                    val withinWindow = isWithinActiveWindow(response.lastSeen)
                    val result = response.isOnline && withinWindow
                    
                    if (!result && response.isOnline) {
                        Napier.d("User $userId is marked online but last_seen is stale (${response.lastSeen})")
                    }
                    
                    result
                }.getOrElse { error ->
                    Napier.w("Failed to fetch presence for user $userId: ${error.message}")
                    false
                }
                emit(isActive)
                delay(10_000) // Poll every 10 seconds
            }
        }
    }
    
    override suspend fun isUserInChat(userId: String, chatId: String): Boolean {
        return runCatching {
            val response = client.postgrest.from("user_presence")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingle<UserPresenceDto>()
            
            response.isOnline && 
            response.currentChatId == chatId && 
            isWithinActiveWindow(response.lastSeen)
        }.getOrDefault(false)
    }
    
    private fun isWithinActiveWindow(lastSeen: String?): Boolean {
        if (lastSeen == null) return false
        return try {
            val lastSeenInstant = kotlinx.datetime.Instant.parse(lastSeen)
            val now = Clock.System.now()
            val diff = now - lastSeenInstant
            diff.inWholeMinutes < 5
        } catch (e: Exception) {
            false
        }
    }
}

@Serializable
private data class UserPresenceDto(
    @SerialName("is_online") val isOnline: Boolean,
    @SerialName("last_seen") val lastSeen: String?,
    @SerialName("current_chat_id") val currentChatId: String? = null
)
