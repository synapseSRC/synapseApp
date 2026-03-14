package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.mapper.toDomain
import com.synapse.social.studioasinc.shared.data.mapper.toDto
import com.synapse.social.studioasinc.shared.data.model.NotificationDto
import com.synapse.social.studioasinc.shared.data.model.NotificationPreferencesDto
import com.synapse.social.studioasinc.shared.data.model.NotificationAnalyticsDto
import com.synapse.social.studioasinc.shared.domain.model.Notification
import com.synapse.social.studioasinc.shared.domain.model.NotificationPreferences
import com.synapse.social.studioasinc.shared.domain.model.NotificationAnalytics
import com.synapse.social.studioasinc.shared.domain.repository.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.json.JsonObject
import io.github.aakira.napier.Napier
import com.synapse.social.studioasinc.shared.core.util.getCurrentIsoTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.synapse.social.studioasinc.shared.util.SynapseIO

import kotlinx.coroutines.launch
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.GlobalScope

class SupabaseNotificationRepository(
    private val supabase: SupabaseClient,
    private val externalScope: CoroutineScope
) : NotificationRepository {

    override suspend fun fetchNotifications(userId: String, limit: Long): List<Notification> {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId != userId) {
            Napier.e("IDOR attempt: User $currentUserId tried to fetch notifications for $userId")
            return emptyList()
        }
        return try {
            supabase.postgrest.from("notifications")
                .select(Columns.raw("*, actor:sender_id(display_name, avatar)")) {
                    filter {
                        eq("recipient_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }
                .decodeList<NotificationDto>()
                .map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e("Failed to fetch notifications for $userId", e)
            emptyList()
        }
    }

    override fun getRealtimeNotifications(userId: String): Flow<Notification> {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId != userId) {
            Napier.e("IDOR attempt: User $currentUserId tried to subscribe to notifications for $userId")
            return emptyFlow()
        }

        return callbackFlow {
            val channel = supabase.realtime.channel("notifications:$userId") {}
            val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                filter("recipient_id", FilterOperator.EQ, userId)
            }

            val collector = launch {
                flow.map { it.decodeRecord<NotificationDto>() }.collect {
                    trySend(it.toDomain())
                }
            }

            launch(Dispatchers.SynapseIO) {
                try {
                    channel.subscribe()
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Napier.e("Failed to subscribe to realtime channel", e)
                        close(e)
                    }
                }
            }

            awaitClose {
                collector.cancel()
                externalScope.launch {
                    try {
                        channel.unsubscribe()
                    } catch (e: Exception) {
                        Napier.w("Failed to unsubscribe from realtime channel", e)
                    }
                }
            }
        }
    }

    override suspend fun markAsRead(userId: String, notificationId: String) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId != userId) {
            Napier.e("IDOR attempt: User $currentUserId tried to mark notification $notificationId as read for $userId")
            return
        }
        try {
            val now = getCurrentIsoTime()
            supabase.postgrest.from("notifications").update(
                mapOf(
                    "is_read" to true,
                    "read_at" to now
                )
            ) {
                filter {
                    eq("id", notificationId)
                    eq("recipient_id", userId)
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to mark notification $notificationId as read", e)
        }
    }

    override suspend fun fetchPreferences(userId: String): NotificationPreferences? {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId != userId) {
            Napier.e("IDOR attempt: User $currentUserId tried to fetch preferences for $userId")
            return null
        }
        return try {
            supabase.postgrest.from("notification_preferences")
                .select() {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<NotificationPreferencesDto>()
                ?.toDomain()
        } catch (e: Exception) {
            Napier.e("Failed to fetch preferences for $userId", e)
            null
        }
    }

    override suspend fun updatePreferences(userId: String, preferences: NotificationPreferences) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId != userId || userId != preferences.userId) {
            Napier.e("IDOR attempt: User $currentUserId tried to update preferences for ${preferences.userId}")
            return
        }
        try {
            supabase.postgrest.from("notification_preferences")
                .upsert(preferences.toDto())
        } catch (e: Exception) {
            Napier.e("Failed to update preferences for ${preferences.userId}", e)
        }
    }

    override suspend fun logAnalytics(analytics: NotificationAnalytics) {
        try {
            supabase.postgrest.from("notification_analytics")
                .insert(analytics.toDto())
        } catch (e: Exception) {
            Napier.e("Failed to log notification analytics", e)
        }
    }

    override suspend fun updateOneSignalPlayerId(userId: String, playerId: String) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        if (currentUserId != userId) {
            Napier.e("IDOR attempt: User $currentUserId tried to update OneSignal ID for $userId")
            return
        }
        try {
            supabase.postgrest.from("users").update(
                mapOf("one_signal_player_id" to playerId)
            ) {
                filter { eq("uid", userId) }
            }
        } catch (e: Exception) {
            Napier.e("Failed to update OneSignal ID for $userId", e)
        }
    }
}
