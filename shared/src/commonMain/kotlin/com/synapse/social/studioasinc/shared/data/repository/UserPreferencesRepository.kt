package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.UserPreferences
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib

class UserPreferencesRepository(private val client: SupabaseClientLib = SupabaseClient.client) {

    suspend fun getSecurityNotificationsEnabled(userId: String): Result<Boolean> {
        return try {
            val response = client.from("user_preferences").select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<UserPreferences>()

            // If no record found, default to true
            Result.success(response?.securityNotificationsEnabled ?: true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setSecurityNotificationsEnabled(userId: String, enabled: Boolean): Result<Unit> {
        return try {
             val preferences = UserPreferences(userId, enabled)
             client.from("user_preferences").upsert(preferences) {
                 onConflict = "user_id"
             }
             Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
