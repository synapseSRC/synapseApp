package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.UserPreferences
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository as IUserPreferencesRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib

class UserPreferencesRepositoryImpl(private val client: SupabaseClientLib = SupabaseClient.client) : IUserPreferencesRepository {

    override suspend fun getSecurityNotificationsEnabled(userId: String): Result<Boolean> {
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

    override suspend fun setSecurityNotificationsEnabled(userId: String, enabled: Boolean): Result<Unit> {
        return try {
             val current = getPreferences(userId).getOrNull()
             val preferences = current?.copy(securityNotificationsEnabled = enabled) ?: UserPreferences(userId, enabled)
             client.from("user_preferences").upsert(preferences) {
                 onConflict = "user_id"
             }
             Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPreferences(userId: String): Result<UserPreferences> {
        return try {
            val response = client.from("user_preferences").select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<UserPreferences>()

            Result.success(response ?: UserPreferences(userId, true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePreferences(userId: String, update: (UserPreferences) -> UserPreferences): Result<Unit> {
        return try {
             val current = getPreferences(userId).getOrNull() ?: UserPreferences(userId, true)
             val updated = update(current)
             client.from("user_preferences").upsert(updated) {
                 onConflict = "user_id"
             }
             Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
