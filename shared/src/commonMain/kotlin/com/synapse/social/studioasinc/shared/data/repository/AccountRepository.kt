package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.auth.SocialProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Facebook
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib

class AccountRepository(private val client: SupabaseClientLib = SupabaseClient.client) {
    private val TAG = "AccountRepository"

    suspend fun getSecurityNotificationsEnabled(): Result<Boolean> {
        return try {
            withContext(Dispatchers.Default) {
                val currentUser = client.auth.currentUserOrNull()
                    ?: throw Exception("User not authenticated")

                val response = client.from("user_preferences").select {
                    filter { eq("user_id", currentUser.id) }
                }.decodeSingleOrNull<JsonObject>()

                val enabled = response?.get("security_notifications_enabled")?.jsonPrimitive?.booleanOrNull ?: true
                Result.success(enabled)
            }
        } catch (e: Exception) {
            Napier.e("Failed to load security notifications settings", e, tag = TAG)
            Result.failure(e)
        }
    }

    suspend fun setSecurityNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val currentUser = client.auth.currentUserOrNull()
                    ?: throw Exception("User not authenticated")

                client.from("user_preferences").upsert(
                    mapOf(
                        "user_id" to currentUser.id,
                        "security_notifications_enabled" to enabled
                    )
                ) {
                    onConflict = "user_id"
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Failed to update security notifications", e, tag = TAG)
            Result.failure(e)
        }
    }

    suspend fun getLinkedIdentities(): Result<List<String>> {
        return try {
            withContext(Dispatchers.Default) {
                val identities = client.from("user_identities")
                    .select()
                    .decodeList<JsonObject>()

                val providers = identities.mapNotNull {
                    it["provider"]?.jsonPrimitive?.contentOrNull
                }
                Result.success(providers)
            }
        } catch (e: Exception) {
            Napier.e("Failed to load linked accounts", e, tag = TAG)
            Result.failure(e)
        }
    }

    suspend fun linkIdentity(provider: SocialProvider): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val supabaseProvider = when (provider) {
                    SocialProvider.GOOGLE -> Google
                    SocialProvider.FACEBOOK -> Facebook
                    SocialProvider.APPLE -> Apple
                    SocialProvider.DISCORD,
                    SocialProvider.GITHUB,
                    SocialProvider.TWITTER,
                    SocialProvider.SPOTIFY,
                    SocialProvider.SLACK -> throw UnsupportedOperationException("${provider.name} linking not supported")
                }
                client.auth.linkIdentity(supabaseProvider)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Failed to link identity: $provider", e, tag = TAG)
            Result.failure(e)
        }
    }

    suspend fun unlinkIdentity(provider: SocialProvider): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                val currentUser = client.auth.currentUserOrNull()
                    ?: throw Exception("User not authenticated")

                val identities = currentUser.identities ?: emptyList()
                val targetIdentity = identities.find {
                    it.provider == provider.name.lowercase()
                }

                if (targetIdentity != null) {
                    client.auth.unlinkIdentity(targetIdentity.id)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("${provider.name} account is not linked"))
                }
            }
        } catch (e: Exception) {
            Napier.e("Failed to unlink identity: $provider", e, tag = TAG)
            Result.failure(e)
        }
    }

    suspend fun changeEmail(newEmail: String): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.auth.updateUser {
                    email = newEmail
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Failed to change email", e, tag = TAG)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            withContext(Dispatchers.Default) {
                client.functions.invoke(function = "delete-account")
                client.auth.signOut()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Napier.e("Failed to delete account", e, tag = TAG)
            Result.failure(e)
        }
    }
}
