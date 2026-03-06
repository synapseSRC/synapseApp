package com.synapse.social.studioasinc.settings

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ApiKeyInfo(
    val id: String,
    val provider: String,
    val keyName: String,
    val isActive: Boolean,
    val usageLimit: Int?,
    val usageCount: Int,
    val createdAt: String
)

@Serializable
data class ApiKeyRequest(
    val provider: String,
    val api_key: String,
    val key_name: String? = null,
    val usage_limit: Int? = null
)

@Serializable
data class ProviderSettings(
    val preferredProvider: String = "platform",
    val fallbackToPlatform: Boolean = true,
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7
)

@Singleton
class ApiKeySettingsService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val _apiKeys = MutableStateFlow<List<ApiKeyInfo>>(emptyList())
    val apiKeys: StateFlow<List<ApiKeyInfo>> = _apiKeys

    private val _providerSettings = MutableStateFlow(ProviderSettings())
    val providerSettings: StateFlow<ProviderSettings> = _providerSettings

    suspend fun loadApiKeys(): Result<List<ApiKeyInfo>> {
        return try {
            val token = supabaseClient.auth.currentAccessTokenOrNull()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = supabaseClient.functions.invoke(
                function = "api-key-manager",
                body = emptyMap<String, String>()
            ).bodyAsText()

            val result = Json.decodeFromString<JsonObject>(response)
            val success = result["success"]?.jsonPrimitive?.booleanOrNull ?: false
            
            if (success) {
                val keysArray = result["api_keys"]?.jsonArray ?: emptyList()
                val keys = keysArray.map { keyElement ->
                    Json.decodeFromJsonElement<ApiKeyInfo>(keyElement)
                }
                _apiKeys.value = keys
                Result.success(keys)
            } else {
                val errorMsg = result["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun storeApiKey(
        provider: String,
        apiKey: String,
        keyName: String? = null,
        usageLimit: Int? = null
    ): Result<String> {
        return try {
            val token = supabaseClient.auth.currentAccessTokenOrNull()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = ApiKeyRequest(
                provider = provider,
                api_key = apiKey,
                key_name = keyName ?: "$provider Key",
                usage_limit = usageLimit
            )

            val response = supabaseClient.functions.invoke(
                function = "api-key-manager",
                body = request
            ).bodyAsText()

            val result = Json.decodeFromString<JsonObject>(response)
            val success = result["success"]?.jsonPrimitive?.booleanOrNull ?: false
            
            if (success) {
                loadApiKeys()
                val message = result["message"]?.jsonPrimitive?.contentOrNull ?: "API key stored successfully"
                Result.success(message)
            } else {
                val errorMsg = result["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteApiKey(keyId: String): Result<String> {
        return try {
            val token = supabaseClient.auth.currentAccessTokenOrNull()
                ?: return Result.failure(Exception("Not authenticated"))

            val response = supabaseClient.functions.invoke(
                function = "api-key-manager",
                body = mapOf("key_id" to keyId)
            ).bodyAsText()

            val result = Json.decodeFromString<JsonObject>(response)
            val success = result["success"]?.jsonPrimitive?.booleanOrNull ?: false
            
            if (success) {
                loadApiKeys()
                val message = result["message"]?.jsonPrimitive?.contentOrNull ?: "API key deleted successfully"
                Result.success(message)
            } else {
                val errorMsg = result["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProviderSettings(settings: ProviderSettings): Result<Unit> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.from("ai_provider_settings").upsert(
                mapOf(
                    "user_id" to userId,
                    "preferred_provider" to settings.preferredProvider,
                    "fallback_to_platform" to settings.fallbackToPlatform,
                    "max_tokens" to settings.maxTokens,
                    "temperature" to settings.temperature,
                    "updated_at" to System.currentTimeMillis()
                )
            )

            _providerSettings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadProviderSettings(): Result<ProviderSettings> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not authenticated"))

            val response = supabaseClient.from("ai_provider_settings")
                .select() {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<JsonObject>()

            val settings = if (response != null) {
                ProviderSettings(
                    preferredProvider = response["preferred_provider"]?.jsonPrimitive?.contentOrNull ?: "platform",
                    fallbackToPlatform = response["fallback_to_platform"]?.jsonPrimitive?.booleanOrNull ?: true,
                    maxTokens = response["max_tokens"]?.jsonPrimitive?.intOrNull ?: 1000,
                    temperature = response["temperature"]?.jsonPrimitive?.doubleOrNull ?: 0.7
                )
            } else {
                ProviderSettings()
            }

            _providerSettings.value = settings
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAvailableProviders(): List<String> {
        return listOf("platform", "openai", "gemini", "anthropic", "openrouter")
    }

    fun getProviderDisplayName(provider: String): String {
        return when (provider) {
            "platform" -> "Synapse (Free)"
            "openai" -> "OpenAI GPT"
            "gemini" -> "Google Gemini"
            "anthropic" -> "Anthropic Claude"
            "openrouter" -> "OpenRouter"
            else -> provider.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }

    suspend fun getUserApiKeys(): List<ApiKeyInfo> {
        return _apiKeys.value
    }

    suspend fun getProviderSettings(): ProviderSettings {
        return _providerSettings.value
    }

    suspend fun addApiKey(provider: String, keyName: String, apiKey: String) {
        storeApiKey(provider, apiKey, keyName, null)
    }

    suspend fun updatePreferredProvider(provider: String) {
        val currentSettings = _providerSettings.value
        updateProviderSettings(currentSettings.copy(preferredProvider = provider))
    }

    suspend fun updateFallbackSetting(fallbackToPlatform: Boolean) {
        val currentSettings = _providerSettings.value
        updateProviderSettings(currentSettings.copy(fallbackToPlatform = fallbackToPlatform))
    }
}
